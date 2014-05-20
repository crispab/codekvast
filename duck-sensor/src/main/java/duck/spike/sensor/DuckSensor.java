package duck.spike.sensor;

import duck.spike.util.Configuration;
import org.aspectj.bridge.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is a Java agent that hooks up Duck to the app.
 * <p/>
 * Usage:
 * Add the following option to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/duck-sensor-n.n-shadow.jar=path/to/duck.config
 * </code></pre>
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "FeatureEnvy"})
public class DuckSensor {

    private static final String MY_NAME = DuckSensor.class.getSimpleName();

    private DuckSensor() {
        // Not possible to instantiate a javaagent
    }

    /**
     * This method is invoked by the JVM as part of bootstrapping the -javaagent
     */
    public static void premain(String args, Instrumentation inst) throws IOException {
        Configuration config = Configuration.parseConfigFile(args);
        System.err.printf("%s initializes with %s%n", MY_NAME, config);

        UsageRegistry.initialize(config);
        System.err.printf("%s loaded, now loading aspectjweaver%n", MY_NAME);
        loadAspectjWeaver(args, inst, config.getPackagePrefix());

        int firstResultInSeconds = createTimerTask(config.getSensorDumpIntervalSeconds());

        System.err.printf("%s is ready to detect used code within(%s..*)%n" +
                                  "First write to %s in %d seconds, after that every %d seconds" +
                                  "--------------------------------------------------------------%n",
                          MY_NAME, config.getPackagePrefix(), config.getDataFile(), firstResultInSeconds,
                          config.getSensorDumpIntervalSeconds()
        );
    }

    private static int createTimerTask(int dumpIntervalSeconds) throws IOException {
        UsageDumpingTimerTask timerTask = new UsageDumpingTimerTask();

        Timer timer = new Timer(MY_NAME, true);

        int initialDelaySeconds = 5;
        timer.scheduleAtFixedRate(timerTask, initialDelaySeconds * 1000L, dumpIntervalSeconds * 1000L);

        Runtime.getRuntime().addShutdownHook(new Thread(timerTask, MY_NAME + " shutdown hook"));

        return initialDelaySeconds;
    }

    private static void loadAspectjWeaver(String args, Instrumentation inst, String packagePrefix) {
        System.setProperty("org.aspectj.weaver.loadtime.configuration", join(createConcreteDuckAspect(packagePrefix),
                                                                             Constants.AOP_USER_XML,
                                                                             Constants.AOP_AJC_XML,
                                                                             Constants.AOP_OSGI_XML));
        org.aspectj.weaver.loadtime.Agent.premain(args, inst);
    }

    private static String join(String... args) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        for (String arg : args) {
            sb.append(delimiter).append(arg);
            delimiter = ";";
        }
        return sb.toString();
    }

    /**
     * Creates a concrete implementation of the AbstractDuckAspect, using the packagePrefix for specifying the
     * abstract pointcut 'scope'.
     *
     * @return A file: URI to a temporary aop-ajc.xml file. The file is deleted on JVM exit.
     */
    private static String createConcreteDuckAspect(String packagePrefix) {
        String xml = String.format(
                "<aspectj>\n"
                        + "  <aspects>\n"
                        + "     <concrete-aspect name='duck.spike.DuckAspect' extends='duck.spike.sensor.AbstractDuckAspect'>\n"
                        + "       <pointcut name='scope' expression='within(%1$s..*)'/>\n"
                        + "     </concrete-aspect>\n"
                        + "  </aspects>\n"
                        + "  <weaver options='-verbose'>\n"
                        + "     <include within='%1$s..*' />\n"
                        + "     <include within='duck.spike..*' />\n"
                        + "  </weaver>\n"
                        + "</aspectj>\n",
                packagePrefix
        );

        try {
            File file = File.createTempFile("duck-sensor", ".xml");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(xml);
            writer.close();
            file.deleteOnExit();
            return "file:" + file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(MY_NAME + " cannot create custom aop-ajc.xml", e);
        }
    }

    private static class UsageDumpingTimerTask extends TimerTask {
        @Override
        public void run() {
            UsageRegistry.instance.dumpDataToDisk();
        }
    }
}
