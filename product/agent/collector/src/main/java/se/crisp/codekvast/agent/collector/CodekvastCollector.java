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
package se.crisp.codekvast.agent.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.Constants;
import se.crisp.codekvast.agent.lib.codebase.CodeBase;
import se.crisp.codekvast.agent.lib.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.config.CollectorConfigLocator;
import se.crisp.codekvast.agent.lib.config.MethodAnalyzer;
import se.crisp.codekvast.agent.lib.io.CodebaseDumpException;
import se.crisp.codekvast.agent.lib.io.CodebaseDumper;
import se.crisp.codekvast.agent.lib.io.impl.NullCodebaseDumperImpl;
import se.crisp.codekvast.agent.lib.util.FileUtils;
import se.crisp.codekvast.agent.lib.util.LogUtil;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;

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
 *             DataDumper dataDumper = ...
 *             CodekvastCollector.initialize(config, dataDumper);
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
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void premain(String args, Instrumentation inst) {
        CollectorConfig config = CollectorConfigFactory.parseCollectorConfig(CollectorConfigLocator.locateConfig(System.out), args, true);

        initialize(config);
    }

    private static CodebaseDumper getCodebaseDumper(@SuppressWarnings("unused") CollectorConfig config) {
        return new NullCodebaseDumperImpl();
    }

    /**
     * Initializes CodekvastCollector. Before this method has been invoked, no method invocations are recorded.
     *
     * @param config The configuration object. May be null, in which case Codekvast is disabled.
     */
    public static void initialize(CollectorConfig config) {
        if (config == null) {
            return;
        }

        if (!InvocationRegistry.instance.isNullRegistry()) {
            // Already initialized from -javaagent. Let it be.
            return;
        }

        InvocationRegistry.initialize(config);

        defineAspectjLoadTimeWeaverConfig(config);

        int codebaseDumpingIntervalSeconds = createCodebaseDumperTimerTask(config, getCodebaseDumper(config));
        int firstResultInSeconds = createInvocationDumperTimerTask(config.getCollectorResolutionSeconds());

        log.info("{} is ready to detect used code within({}..*).", NAME, getNormalizedPackages(config));
        log.info("An attempt to upload the codebase will be done every {} seconds until either rejected or successful.",
                 codebaseDumpingIntervalSeconds);
        log.info("First result will be uploaded in {} seconds, thereafter every {} seconds.", firstResultInSeconds,
                 config.getCollectorResolutionSeconds());
    }

    private static String getNormalizedPackages(CollectorConfig config) {
        List<String> prefixes = config.getNormalizedPackages();
        return prefixes.size() == 1 ? prefixes.get(0) : prefixes.toString();
    }

    private static int createCodebaseDumperTimerTask(CollectorConfig config, CodebaseDumper codebaseDumper) {
        Timer timer = new Timer(NAME + " Codebase Dumper", true);

        CodebaseDumpingTimerTask timerTask = new CodebaseDumpingTimerTask(timer, config, codebaseDumper);

        int initialDelaySeconds = 10;
        int periodSeconds = 60;
        timer.scheduleAtFixedRate(timerTask, initialDelaySeconds * 1000L, periodSeconds * 1000L);

        return periodSeconds;
    }

    private static int createInvocationDumperTimerTask(int dumpIntervalSeconds) {
        Timer timer = new Timer(NAME + " Invocation Dumper", true);

        InvocationDumpingTimerTask timerTask = new InvocationDumpingTimerTask(timer);

        int initialDelaySeconds = 5;
        timer.scheduleAtFixedRate(timerTask, initialDelaySeconds * 1000L, dumpIntervalSeconds * 1000L);

        Runtime.getRuntime().addShutdownHook(new Thread(timerTask, NAME + " shutdown hook"));

        return initialDelaySeconds;
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

        String xml = String.format(
            "<aspectj>\n"
                + "  <aspects>\n"
                + "    <concrete-aspect name='se.crisp.codekvast.agent.collector.MethodExecutionAspect'\n"
                + "                     extends='%1$s'>\n"
                + "      <pointcut name='methodExecution' expression='%2$s'/>\n"
                + "    </concrete-aspect>\n"
                + "  </aspects>\n"
                + "  <weaver options='%3$s'>\n"
                + "%4$s"
                + "%5$s"
                + "  </weaver>\n"
                + "</aspectj>\n",
            AbstractMethodExecutionAspect.class.getName(),
            toMethodExecutionPointcut(config.getMethodAnalyzer()),
            config.getAspectjOptions(),
            getIncludeExcludeElements("include", config.getNormalizedPackages()),
            getIncludeExcludeElements("exclude", config.getNormalizedExcludePackages(),
                                      "se.crisp.codekvast.agent"));

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

    @RequiredArgsConstructor
    private static class CodebaseDumpingTimerTask extends TimerTask {
        private final Timer timer;
        private final CollectorConfig config;
        private final CodebaseDumper codebaseDumper;

        private CodeBase codeBase;

        @Override
        public void run() {
            if (codeBase == null) {
                log.info("Building codebase");
                codeBase = new CodeBase(config);
            }
            try {
                if (codebaseDumper.needsToBeDumped(codeBase.getFingerprint())) {
                    CodeBaseScanner scanner = new CodeBaseScanner();
                    scanner.scanSignatures(codeBase);
                    codebaseDumper.dumpCodebase(codeBase);
                    log.info("Dumped codebase {}", codeBase.getFingerprint());
                } else {
                    log.info("Codebase {} already dumped", codeBase.getFingerprint());
                }
                timer.cancel();
            } catch (CodebaseDumpException e) {
                LogUtil.logException(log, "Cannot dump codebase", e);
            }
        }
    }

    @RequiredArgsConstructor
    private static class InvocationDumpingTimerTask extends TimerTask {

        private final Timer timer;
        private int dumpCount;

        @Override
        public void run() {
            if (InvocationRegistry.instance.isNullRegistry()) {
                // Someone has pulled the carpet...
                log.info("{} has been disabled, stopping timer task", NAME);
                timer.cancel();
            } else {
                dumpCount += 1;
                InvocationRegistry.instance.dumpData(dumpCount);
            }
        }
    }
}
