package duck.spike;

import org.aspectj.bridge.Constants;
import org.aspectj.weaver.loadtime.Agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a Java agent that hooks up Duck to the app.
 * <p/>
 * Usage:
 * Add the following option to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/duck-agent-n.n-shadow.jar=packagePrefix=com.acme
 * </code></pre>
 *
 * @author Olle Hallin
 */
public class DuckAgent {

    private static final String MY_SIMPLE_NAME = DuckAgent.class.getSimpleName();

    private DuckAgent() {
        // Not possible to instantiate a javaagent
    }

    /**
     * This method is invoked by the JVM as part of the bootstrapping
     */
    public static void premain(String args, Instrumentation inst) throws IOException {
        String packagePrefix = parsePackagePrefix(args);
        File outputFile = parseOutputFile(args);

        UsageRegistry.initialize(packagePrefix, outputFile);

        loadAspectjWeaver(args, inst, packagePrefix);

        createUsageDumpers(parseDumpIntervalSeconds(args));

        System.err.printf("%s is ready to detect useless code within(%s..*)%n" +
                                  "Now handing over to main()%n" +
                                  "--------------------------------------------------------------%n",
                          MY_SIMPLE_NAME, packagePrefix
        );
    }

    private static void createUsageDumpers(int dumpIntervalSeconds) throws IOException {
        UsageDumper usageDumper = new UsageDumper();

        Timer timer = new Timer("Duck usage dumper", true);

        long dumpDelayMillis = dumpIntervalSeconds * 1000L;
        timer.scheduleAtFixedRate(usageDumper, dumpDelayMillis, dumpDelayMillis);

        Thread shutdownHook = new Thread(usageDumper);
        shutdownHook.setName("Duck shutdown hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void loadAspectjWeaver(String args, Instrumentation inst, String packagePrefix) {
        System.err.printf("%s loaded, now loading aspectjweaver%n", MY_SIMPLE_NAME);
        System.setProperty("org.aspectj.weaver.loadtime.configuration", join(createConcreteDuckAspect(packagePrefix),
                                                                             Constants.AOP_USER_XML,
                                                                             Constants.AOP_AJC_XML,
                                                                             Constants.AOP_OSGI_XML));
        Agent.premain(args, inst);
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
     * @return A file: URL to a temporary aop-ajc.xml file. The file is deleted on JVM exit.
     */
    private static String createConcreteDuckAspect(String packagePrefix) {
        String xml = String.format(
                "<aspectj>\n"
                        + "  <aspects>\n"
                        + "     <concrete-aspect name='duck.spike.DuckAspect' extends='duck.spike.AbstractDuckAspect'>\n"
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
            File file = File.createTempFile("aop-duck", ".xml");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(xml);
            writer.close();
            file.deleteOnExit();
            return "file:" + file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create custom aspect XML", e);
        }
    }

    private static String parsePackagePrefix(CharSequence args) {
        Pattern pattern = Pattern.compile(".*packagePrefix=([^,]+).*");
        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Usage: javaagent:/path/to/duck-agent.jar=packagePrefix=<package>");
        }
        return matcher.group(1);
    }

    private static int parseDumpIntervalSeconds(CharSequence args) {
        Pattern pattern = Pattern.compile(".*dumpIntervalSeconds=([\\d]+).*");
        Matcher matcher = pattern.matcher(args);
        String result = matcher.matches() ? matcher.group(1) : "600";
        System.err.printf("Will dump usage data every %s seconds%n", result);
        return Integer.parseInt(result);
    }

    private static File parseOutputFile(String args) {
        Pattern pattern = Pattern.compile(".*outputFile=([^,]+).*");
        Matcher matcher = pattern.matcher(args);
        File result = matcher.matches() ? new File(matcher.group(1)) : new File("duck-data.txt");
        System.err.printf("Will dump usage data to %s%n", result.getAbsolutePath());
        return result;
    }

    private static class UsageDumper extends TimerTask {
        public void run() {
            UsageRegistry.dumpCodeUsage();
        }
    }
}
