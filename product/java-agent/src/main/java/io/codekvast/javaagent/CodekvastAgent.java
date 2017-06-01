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
package io.codekvast.javaagent;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.config.AgentConfigLocator;
import io.codekvast.javaagent.config.MethodAnalyzer;
import io.codekvast.javaagent.publishing.impl.CodeBasePublisherFactoryImpl;
import io.codekvast.javaagent.publishing.impl.InvocationDataPublisherFactoryImpl;
import io.codekvast.javaagent.scheduler.Scheduler;
import io.codekvast.javaagent.scheduler.impl.ConfigPollerImpl;
import io.codekvast.javaagent.util.FileUtils;
import lombok.extern.java.Log;
import org.aspectj.bridge.Constants;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the Java javaagent that hooks up Codekvast to the app.
 * <p>
 * Invocation: Add the following options to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/codekvast-agent-n.n.jar -Xbootclasspath/a:/path/to/codekvast-agent-n.n.jar
 * </code></pre>
 * <p>
 * CodekvastAgent could also be initialized from a statically woven aspect.
 * <p>
 * In that case, the aspect should have a static block that locates the config and initializes the agent:
 * <pre><code>
 *     public aspect MethodExecutionAspect extends AbstractMethodExecutionAspect {
 *
 *         static {
 *             AgentConfig config = ...
 *             CodekvastAgent.initialize(config);
 *         }
 *
 *         public pointcut methodExecution: execution(public * *..*(..)) &amp;&amp; within(foo..*)
 *
 *     }
 * </code></pre>
 *
 * @author olle.hallin@crisp.se
 */
@Log
public class CodekvastAgent {

    private static final String NAME = "Codekvast";

    // AspectJ uses this system property for defining the list of names of load-time weaving config files to locate...
    private static final String ASPECTJ_WEAVER_CONFIGURATION = "org.aspectj.weaver.loadtime.configuration";

    @Nullable
    private static Scheduler scheduler;

    private CodekvastAgent() {
        // Not possible to instantiate a javaagent
    }

    /**
     * This method is invoked by the JVM as part of bootstrapping the -javaagent
     *
     * @param args            The string after the equals sign in -javaagent:codekvast-agent.jar=args. Is used as overrides to the agent
     *                        configuration file.
     * @param instrumentation The standard instrumentation hook.
     */
    public static void premain(String args, Instrumentation instrumentation) {
        AgentConfig config = AgentConfigFactory.parseAgentConfig(AgentConfigLocator.locateConfig(), args, true);

        initialize(config);

        if (config != null) {
            org.aspectj.weaver.loadtime.Agent.premain(args, instrumentation);
        }
    }

    /**
     * Initializes CodekvastAgent. Before this method has been invoked, no method invocations are recorded.
     *
     * @param config The configuration object. May be null, in which case Codekvast is disabled.
     */
    public static void initialize(AgentConfig config) {
        if (config == null) {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
            InvocationRegistry.initialize(null);
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
                                  new InvocationDataPublisherFactoryImpl())
            .start();

        Runtime.getRuntime().addShutdownHook(new MyShutdownHook());

        log.info(String.format("%s is ready to detect used code in %s %s within %s.", NAME, config.getAppName(),
                               config.getResolvedAppVersion(), getPrettyPackages(config)));
    }

    private static String getPrettyPackages(AgentConfig config) {
        List<String> prefixes = config.getNormalizedPackages();
        return prefixes.size() == 1 ? "package " + prefixes.get(0) : "packages " + prefixes.toString();
    }

    private static void defineAspectjLoadTimeWeaverConfig(AgentConfig config) {
        try {
            Class.forName("org.aspectj.bridge.Constants");

            System.setProperty(ASPECTJ_WEAVER_CONFIGURATION,
                               createAopXml(config) + ";" +
                                   Constants.AOP_USER_XML + ";" +
                                   Constants.AOP_AJC_XML + ";" +
                                   Constants.AOP_OSGI_XML);

            log.fine(ASPECTJ_WEAVER_CONFIGURATION + "=" + System.getProperty(ASPECTJ_WEAVER_CONFIGURATION));
        } catch (ClassNotFoundException e) {
            log.warning("Not using AspectJ load-time weaving.");
        }
    }

    /**
     * Creates a concrete implementation of the AbstractMethodExecutionAspect, using the packages for specifying the abstract
     * pointcut 'scope'.
     *
     * @return A file URI to a temporary aop-ajc.xml file.
     */
    private static String createAopXml(AgentConfig config) {
        String messageHandlerClass = config.isBridgeAspectjMessagesToJUL()
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
                                      "io.codekvast.javaagent", "ck"));
        log.finest("aop.xml=" + xml);
        File file = config.getAspectFile();
        FileUtils.writeToFile(xml, file);
        return "file:" + file.getAbsolutePath();
    }

    private static String getIncludeExcludeElements(String element, List<String> packages, String... extraPrefixes) {
        StringBuilder sb = new StringBuilder();

        Set<String> prefixes = new HashSet<>(packages);
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

    private static class MyShutdownHook extends Thread {

        MyShutdownHook() {
            setName(NAME + " shutdown hook");

            setContextClassLoader(null);

            //noinspection InnerClassTooDeeplyNested,AnonymousInnerClass
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @SuppressWarnings("UseOfSystemOutOrSystemErr")
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.err.println(NAME + " Uncaught exception in  " + t.getName());
                    e.printStackTrace(System.err);
                }
            });
        }

        @Override
        public void run() {
            // Cannot use logger here, since logging could have been shut down already

            //noinspection UseOfSystemOutOrSystemErr
            System.err.println(NAME + " is shutting down...");
            long startedAt = System.currentTimeMillis();

            initialize(null);

            long elapsed = System.currentTimeMillis() - startedAt;

            //noinspection UseOfSystemOutOrSystemErr
            System.err.println(NAME + " shutdown completed in " + elapsed + " ms");
        }
    }
}
