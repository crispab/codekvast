package se.crisp.codekvast.agent.collector;

import org.aspectj.bridge.Constants;
import se.crisp.codekvast.agent.collector.aspects.AbstractMethodExecutionAspect;
import se.crisp.codekvast.agent.collector.aspects.JasperExecutionAspect;
import se.crisp.codekvast.agent.util.CollectorConfig;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the Java agent that hooks up Codekvast to the app. It also loads aspectjweaver.
 * <p/>
 * Usage: Add the following option to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/codekvast-collector-n.n-shadow.jar=path/to/codekvast.conf
 * </code></pre>
 *
 * @author Olle Hallin
 */
public class CodekvastCollector {

    public static final String NAME = "Codekvast";
    private static final String ASPECTJ_WEAVER_CONFIGURATION = "org.aspectj.weaver.loadtime.configuration";

    public static PrintStream out;

    private CodekvastCollector() {
        // Not possible to instantiate a javaagent
    }

    /**
     * This method is invoked by the JVM as part of bootstrapping the -javaagent
     */
    public static void premain(String args, Instrumentation inst) {
        CollectorConfig config = CollectorConfig.parseConfigFile(args);

        //noinspection UseOfSystemOutOrSystemErr
        CodekvastCollector.out = config.isVerbose() ? System.err : new PrintStream(new NullOutputStream());

        UsageRegistry.initialize(config);

        loadAspectjWeaver(args, inst, config);

        int firstResultInSeconds = createTimerTask(config.getCollectorResolutionSeconds());

        CodekvastCollector.out.printf("%s is ready to detect used code within(%s..*).%n" +
                                              "First write to %s will be in %d seconds, thereafter every %d seconds.%n" +
                                              "-------------------------------------------------------------------------------%n",
                                      NAME, config.getPackagePrefix(), config.getUsageFile(), firstResultInSeconds,
                                      config.getCollectorResolutionSeconds()
        );
    }

    private static int createTimerTask(int dumpIntervalSeconds) {
        UsageDumpingTimerTask timerTask = new UsageDumpingTimerTask();

        Timer timer = new Timer(NAME, true);

        int initialDelaySeconds = 5;
        timer.scheduleAtFixedRate(timerTask, initialDelaySeconds * 1000L, dumpIntervalSeconds * 1000L);

        Runtime.getRuntime().addShutdownHook(new Thread(timerTask, NAME + " shutdown hook"));

        return initialDelaySeconds;
    }

    private static void loadAspectjWeaver(String args, Instrumentation inst, CollectorConfig config) {
        System.setProperty(ASPECTJ_WEAVER_CONFIGURATION, join(";", createAopXml(config),
                                                                             Constants.AOP_USER_XML,
                                                                             Constants.AOP_AJC_XML,
                                                                             Constants.AOP_OSGI_XML));

        CodekvastCollector.out.printf("%s=%s%n", ASPECTJ_WEAVER_CONFIGURATION, System.getProperty(ASPECTJ_WEAVER_CONFIGURATION));
        if (config.isInvokeAspectjWeaver()) {
            CodekvastCollector.out.printf("%s is invoking aspectjweaver%n", NAME);
            org.aspectj.weaver.loadtime.Agent.premain(args, inst);
        } else {
            CodekvastCollector.out.printf("%s is NOT invoking aspectjweaver%n", NAME);
        }
    }

    private static String join(String delimiter, String... args) {
        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String arg : args) {
            sb.append(delim).append(arg);
            delim = delimiter;
        }
        return sb.toString();
    }

    /**
     * Creates a concrete implementation of the AbstractMethodExecutionAspect, using the packagePrefix for specifying the abstract pointcut
     * 'scope'.
     *
     * @return A file URI to a temporary aop-ajc.xml file. The file is deleted on JVM exit.
     */
    private static String createAopXml(CollectorConfig config) {
        String xml = String.format(
                "<aspectj>\n"
                        + "  <aspects>\n"
                        + "    <aspect name='%1$s'/>\n"
                        + "    <concrete-aspect name='se.crisp.codekvast.agent.collector.aspects.PublicMethodExecutionAspect'\n"
                        + "                     extends='%2$s'>\n"
                        + "      <pointcut name='scope' expression='within(%3$s..*)'/>\n"
                        + "    </concrete-aspect>\n"
                        + "  </aspects>\n"
                        + "  <weaver options='%4$s'>\n"
                        + "    <include within='%3$s..*' />\n"
                        + "    <include within='%5$s..*' />\n"
                        + "  </weaver>\n"
                        + "</aspectj>\n",
                JasperExecutionAspect.class.getName(),
                AbstractMethodExecutionAspect.class.getName(),
                config.getNormalizedPackagePrefix(),
                config.getAspectjOptions() + " -XmessageHandlerClass:" + AspectjMessageHandler.class.getName(),
                JasperExecutionAspect.JASPER_BASE_PACKAGE
        );

        File file = config.getAspectFile();
        if (config.isClobberAopXml() || !file.canRead()) {
            FileUtils.writeToFile(xml, file);
        }
        return "file:" + file.getAbsolutePath();
    }

    private static class UsageDumpingTimerTask extends TimerTask {

        private int dumpCount;

        @Override
        public void run() {
            dumpCount += 1;
            UsageRegistry.instance.dumpDataToDisk(dumpCount);
        }
    }
}
