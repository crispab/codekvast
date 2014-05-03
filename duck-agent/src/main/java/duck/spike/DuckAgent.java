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
 * This is a Java agent that hooks up duck to the app.
 * Usage:
 * Add the following line to the Java command line:
 * <pre><code>
 *    -javaagent:/path/to/duck-agent-shadow.jar=packagePrefix=packagePrefix
 * </code></pre>
 *
 * @author Olle Hallin
 */
public class DuckAgent {

    private DuckAgent() {
        // Not possible to instantiate a javaagent
    }

    public static void premain(String args, Instrumentation inst) {
        String packagePrefix = parsePackagePrefix(args);

        UsageRegistry.setPackagePrefix(packagePrefix);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                UsageRegistry.dumpUnusedCode();
            }
        }));

        System.setProperty("org.aspectj.weaver.loadtime.configuration", Constants.AOP_USER_XML + ";" + Constants.AOP_AJC_XML + ";" +
                Constants.AOP_OSGI_XML + ";" + createConcreteDuckAspect(packagePrefix));

        System.out.printf("DuckAgent will detect useless code in packages %s..*" +
                "\nnow delegating to AspectJ load-time weaver agent%n", packagePrefix);

        Agent.premain(args, inst);
    }

    private static String createConcreteDuckAspect(String packagePrefix) {
        String xml = String.format("<aspectj>\n"
                + "  <aspects>\n"
                + "     <concrete-aspect name='duck.spike.DuckAspect' extends='duck.spike.AbstractDuckAspect'>\n"
                + "       <pointcut name='scope' expression='within(%1$s..*)'/>\n"
                + "     </concrete-aspect>\n"
                + "  </aspects>\n"
                + "  <weaver options='-verbose'>\n"
                + "     <include within='%1$s..*' />\n"
                + "     <include within='duck.spike..*' />\n"
                + "  </weaver>\n"
                + "</aspectj>\n", packagePrefix);

        try {
            File file = File.createTempFile("duck", ".xml");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(xml);
            writer.close();
            file.deleteOnExit();
            return "file:" + file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create custom aspect", e);
        }
    }

    private static String parsePackagePrefix(String args) {
        Pattern pattern = Pattern.compile("packagePrefix=([\\w.]+)");
        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Usage: javaagent:/path/to/duck-agent.jar=packagePrefix=<package>");
        }
        return matcher.group(1);
    }
}
