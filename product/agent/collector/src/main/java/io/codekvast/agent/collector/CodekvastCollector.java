/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.agent.collector;

import io.codekvast.agent.collector.io.impl.CodeBasePublisherFactoryImpl;
import io.codekvast.agent.collector.io.impl.InvocationDataPublisherFactoryImpl;
import io.codekvast.agent.collector.scheduler.impl.ConfigPollerImpl;
import io.codekvast.agent.collector.scheduler.Scheduler;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import io.codekvast.agent.lib.config.CollectorConfigLocator;
import io.codekvast.agent.lib.config.MethodAnalyzer;
import io.codekvast.agent.lib.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.Constants;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This is the Java agent that hooks up Codekvast to the app.
 * <p>
 * Invocation: Add the following options to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/codekvast-collector-n.n.jar -javaagent:/path/to/aspectjweaver-n.n.jar
 * </code></pre>
 * <p>
 * <em>NOTE: the ordering of the collector and the aspectjweaver is important!</em>
 * <p>
 * CodekvastCollector could also be initialized from a statically woven aspect.
 * <p>
 * In that case, the aspect should have a static block that locates the config and initializes the collector:
 * <pre><code>
 *     public aspect MethodExecutionAspect extends AbstractMethodExecutionAspect {
 *
 *         static {
 *             CollectorConfig config = ...
 *             CodekvastCollector.initialize(config);
 *         }
 *
 *         public pointcut methodExecution: execution(public * *..*(..)) &amp;&amp; within(foo..*)
 *
 *     }
 * </code></pre>
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class CodekvastCollector {

    private static final String NAME = "Codekvast";

    // AspectJ uses this system property for defining the list of names of load-time weaving config files to locate...
    private static final String ASPECTJ_WEAVER_CONFIGURATION = "org.aspectj.weaver.loadtime.configuration";

    private static Scheduler scheduler;

    private CodekvastCollector() {
        // Not possible to instantiate a javaagent
    }

    /**
     * This method is invoked by the JVM as part of bootstrapping the -javaagent
     *
     * @param args The string after the equals sign in -javaagent:codekvast-collector.jar=args. Is used as overrides to the collector
     *             configuration file.
     * @param inst The standard instrumentation hook.
     */
    public static void premain(String args, Instrumentation inst) {
        CollectorConfig config = CollectorConfigFactory.parseCollectorConfig(CollectorConfigLocator.locateConfig(), args, true);

        initialize(config);
    }

    /**
     * Initializes CodekvastCollector. Before this method has been invoked, no method invocations are recorded.
     *
     * @param config The configuration object. May be null, in which case Codekvast is disabled.
     */
    public static void initialize(CollectorConfig config) {
        if (config == null) {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
            return;
        }

        if (scheduler != null) {
            // Already initialized from -javaagent. Let it be.
            return;
        }

        InvocationRegistry.initialize(config);

        defineAspectjLoadTimeWeaverConfig(config);

        scheduler = new Scheduler(config,
                                  new ConfigPollerImpl(config),
                                  new CodeBasePublisherFactoryImpl(),
                                  new InvocationDataPublisherFactoryImpl());

        scheduler.start(10, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(createShutdownHook());

        log.info("{} is ready to detect used code within({}..*).", NAME, getNormalizedPackages(config));
    }

    private static Thread createShutdownHook() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                scheduler.shutdown();
            }
        });
        thread.setName(NAME + " Shutdown Hook");
        return thread;
    }

    private static String getNormalizedPackages(CollectorConfig config) {
        List<String> prefixes = config.getNormalizedPackages();
        return prefixes.size() == 1 ? prefixes.get(0) : prefixes.toString();
    }

    private static void defineAspectjLoadTimeWeaverConfig(CollectorConfig config) {
        try {
            Class.forName("org.aspectj.bridge.Constants");

            System.setProperty(ASPECTJ_WEAVER_CONFIGURATION,
                               createAopXml(config) + ";" +
                                   Constants.AOP_USER_XML + ";" +
                                   Constants.AOP_AJC_XML + ";" +
                                   Constants.AOP_OSGI_XML);

            log.debug("{}={}", ASPECTJ_WEAVER_CONFIGURATION, System.getProperty(ASPECTJ_WEAVER_CONFIGURATION));
        } catch (ClassNotFoundException e) {
            log.warn("Not using AspectJ load-time weaving.");
        }
    }

    /**
     * Creates a concrete implementation of the AbstractMethodExecutionAspect, using the packages for specifying the abstract
     * pointcut 'scope'.
     *
     * @return A file URI to a temporary aop-ajc.xml file.
     */
    private static String createAopXml(CollectorConfig config) {
        String messageHandlerClass = config.isBridgeAspectjMessagesToSLF4J()
            ? String.format("-XmessageHandlerClass:%s ", AspectjMessageHandler.class.getName())
            : "";

        String xml = String.format(
            "<aspectj>\n"
                + "  <aspects>\n"
                + "    <concrete-aspect name='%1$s.MethodExecutionAspect'\n"
                + "                     extends='%2$s'>\n"
                + "      <pointcut name='methodExecution' expression='%3$s'/>\n"
                + "    </concrete-aspect>\n"
                + "  </aspects>\n"
                + "  <weaver options='%4$s'>\n"
                + "%5$s"
                + "%6$s"
                + "  </weaver>\n"
                + "</aspectj>\n",
            AbstractMethodExecutionAspect.class.getPackage().getName(),
            AbstractMethodExecutionAspect.class.getName(),
            toMethodExecutionPointcut(config.getMethodAnalyzer()),
            messageHandlerClass + config.getAspectjOptions(),
            getIncludeExcludeElements("include", config.getNormalizedPackages()),
            getIncludeExcludeElements("exclude", config.getNormalizedExcludePackages(),
                                      "io.codekvast.agent"));

        File file = config.getAspectFile();
        if (config.isClobberAopXml() || !file.canRead()) {
            FileUtils.writeToFile(xml, file);
        }
        return "file:" + file.getAbsolutePath();
    }

    private static String getIncludeExcludeElements(String element, List<String> packages, String... extraPrefixes) {
        StringBuilder sb = new StringBuilder();

        Set<String> prefixes = new HashSet<String>(packages);
        Collections.addAll(prefixes, extraPrefixes);

        for (String prefix : prefixes) {
            sb.append(String.format("    <%s within='%s..*' />\n", element, prefix));
        }
        return sb.toString();
    }

    private static String toMethodExecutionPointcut(MethodAnalyzer filter) {
        if (filter.selectsPrivateMethods()) {
            return "execution(* *..*(..)) || execution(*..new(..))";
        }
        if (filter.selectsPackagePrivateMethods()) {
            return "execution(!private * *..*(..)) || execution(!private *..new(..))";
        }
        if (filter.selectsProtectedMethods()) {
            return "execution(public * *..*(..)) || execution(protected * *..*(..)) " +
                "|| execution(public *..new(..)) || execution(protected *..new(..))";
        }
        return "execution(public * *..*(..)) || execution(public *..new(..))";
    }
}
