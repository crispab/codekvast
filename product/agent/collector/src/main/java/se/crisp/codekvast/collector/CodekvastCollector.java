package se.crisp.codekvast.collector;

import org.aspectj.bridge.Constants;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.config.CollectorConfigLocator;
import se.crisp.codekvast.shared.config.MethodFilter;
import se.crisp.codekvast.shared.io.FileSystemInvocationDataDumper;
import se.crisp.codekvast.shared.io.InvocationDataDumper;
import se.crisp.codekvast.shared.util.FileUtils;

import java.io.File;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
public class CodekvastCollector {

    public static final String NAME = "Codekvast";

    // AspectJ uses this system property for defining the list of names of load-time weaving config files to locate...
    private static final String ASPECTJ_WEAVER_CONFIGURATION = "org.aspectj.weaver.loadtime.configuration";

    public static PrintStream out;

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
        CollectorConfig config = CollectorConfig.parseCollectorConfig(CollectorConfigLocator.locateConfig(System.out), args, true);

        initialize(config, new FileSystemInvocationDataDumper(config, out));
    }

    /**
     * Initializes CodekvastCollector. Before this method has been invoked, no method invocations are recorded.
     *
     * @param config     The configuration object. May be null, in which case Codekvast is disabled.
     * @param dataDumper The strategy for how to dump data.
     */
    public static void initialize(CollectorConfig config, InvocationDataDumper dataDumper) {
        if (InvocationRegistry.instance != null && config != null) {
            // Already initialized from -javaagent. Let it be.
            return;
        }

        InvocationRegistry.initialize(config, dataDumper);

        if (config == null) {
            return;
        }

        CodekvastCollector.out = config.isVerbose() ? System.err : new PrintStream(new NullOutputStream());

        defineAspectjLoadTimeWeaverConfig(config);

        int firstResultInSeconds = createTimerTask(config.getCollectorResolutionSeconds());

        CodekvastCollector.out.printf("%s is ready to detect used code within(%s..*).%n" +
                                              "First write to %s will be in %d seconds, thereafter every %d seconds.%n" +
                                              "-------------------------------------------------------------------------------%n",
                                      NAME, getNormalizedPackagePrefixes(config), config.getInvocationsFile(),
                                      firstResultInSeconds, config.getCollectorResolutionSeconds()
        );
    }

    private static String getNormalizedPackagePrefixes(CollectorConfig config) {
        List<String> prefixes = config.getNormalizedPackagePrefixes();
        return prefixes.size() == 1 ? prefixes.get(0) : prefixes.toString();
    }

    private static int createTimerTask(int dumpIntervalSeconds) {
        Timer timer = new Timer(NAME, true);

        InvocationDumpingTimerTask timerTask = new InvocationDumpingTimerTask(timer);

        int initialDelaySeconds = 5;
        timer.scheduleAtFixedRate(timerTask, initialDelaySeconds * 1000L, dumpIntervalSeconds * 1000L);

        Runtime.getRuntime().addShutdownHook(new Thread(timerTask, NAME + " shutdown hook"));

        return initialDelaySeconds;
    }

    private static void defineAspectjLoadTimeWeaverConfig(CollectorConfig config) {
        try {
            Class.forName("org.aspectj.bridge.Constants");

            System.setProperty(ASPECTJ_WEAVER_CONFIGURATION, createAopXml(config) + ";" +
                    Constants.AOP_USER_XML + ";" +
                    Constants.AOP_AJC_XML + ";" +
                    Constants.AOP_OSGI_XML);

            CodekvastCollector.out.printf("%s=%s%n", ASPECTJ_WEAVER_CONFIGURATION, System.getProperty(ASPECTJ_WEAVER_CONFIGURATION));
        } catch (ClassNotFoundException e) {
            CodekvastCollector.out.printf("Not using AspectJ load-time weaving.%n");
        }
    }

    /**
     * Creates a concrete implementation of the AbstractMethodExecutionAspect, using the packagePrefixes for specifying the abstract
     * pointcut 'scope'.
     *
     * @return A file URI to a temporary aop-ajc.xml file.
     */
    private static String createAopXml(CollectorConfig config) {
        String aspectjOptions = config.getAspectjOptions();
        if (aspectjOptions == null) {
            aspectjOptions = "";
        }

        StringBuilder includeWithin = new StringBuilder();
        for (String prefix : config.getNormalizedPackagePrefixes()) {
            includeWithin.append(String.format("    <include within='%s..*' />\n", prefix));
        }

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
                        + "    <exclude within='%5$s..*'/>\n"
                        + "  </weaver>\n"
                        + "</aspectj>\n",
                AbstractMethodExecutionAspect.class.getName(),
                toMethodExecutionPointcut(config.getMethodVisibility()),
                aspectjOptions,
                includeWithin.toString(),
                CodekvastCollector.class.getPackage().getName()
        );

        File file = config.getAspectFile();
        if (config.isClobberAopXml() || !file.canRead()) {
            FileUtils.writeToFile(xml, file);
        }
        return "file:" + file.getAbsolutePath();
    }

    private static String toMethodExecutionPointcut(MethodFilter filter) {
        if (filter.selectsPrivateMethods()) {
            return "execution(* *..*(..))";
        }
        if (filter.selectsPackagePrivateMethods()) {
            return "execution(!private * *..*(..))";
        }
        if (filter.selectsProtectedMethods()) {
            return "execution(public * *..*(..)) || execution(protected * *..*(..))";
        }
        return "execution(public * *..*(..))";
    }

    private static class InvocationDumpingTimerTask extends TimerTask {

        private final Timer timer;
        private int dumpCount;

        private InvocationDumpingTimerTask(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void run() {
            if (InvocationRegistry.instance == null) {
                // Someone has pulled the carpet...
                timer.cancel();
            } else {
                dumpCount += 1;
                InvocationRegistry.instance.dumpData(dumpCount);
            }
        }
    }
}
