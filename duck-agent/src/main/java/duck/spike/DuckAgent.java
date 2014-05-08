package duck.spike;

import org.aspectj.bridge.Constants;
import org.aspectj.weaver.loadtime.Agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a Java agent that hooks up Duck to the app.
 *
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
        final String packagePrefix = parsePackagePrefix(args);

        loadAspectjWeaver(args, inst, packagePrefix);
        createUsageDumperThread(packagePrefix);

        System.out.printf("%s is ready to detect useless code within(%s..*)%n" +
                                  "Now handing over to main()%n" +
                                  "--------------------------------------------------------------%n",
                          MY_SIMPLE_NAME, packagePrefix
        );
    }

    private static void createUsageDumperThread(final String packagePrefix) throws IOException {
        Thread usageDumper = new Thread(new UsageDumper(packagePrefix));
        usageDumper.setName("Duck Usage Dumper");
        Runtime.getRuntime().addShutdownHook(usageDumper);
    }

    private static void loadAspectjWeaver(String args, Instrumentation inst, String packagePrefix) {
        System.out.printf("%s loaded, now loading aspectjweaver%n", MY_SIMPLE_NAME);
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
        Pattern pattern = Pattern.compile("packagePrefix=([\\w.]+)");
        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Usage: javaagent:/path/to/duck-agent.jar=packagePrefix=<package>");
        }
        return matcher.group(1);
    }

    private static class UsageDumper implements Runnable {
        private final String packagePrefix;

        private UsageDumper(String packagePrefix) {
            this.packagePrefix = packagePrefix;
        }

        public void run() {
            UsageRegistry.dumpUnusedCode(packagePrefix);
        }
    }
}
