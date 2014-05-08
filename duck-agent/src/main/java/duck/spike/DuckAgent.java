package duck.spike;

import org.aspectj.bridge.Constants;
import org.aspectj.weaver.loadtime.Agent;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
        installPrivateClassLoader();
        try {
            String packagePrefix = parsePackagePrefix(args);
            loadAspectjWeaver(args, inst, packagePrefix);

            System.out.printf("%s will now scan classpath for packagePrefix '%s'%n", MY_SIMPLE_NAME, packagePrefix);
            UsageRegistry.scanClasspath(packagePrefix);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    UsageRegistry.dumpUnusedCode();
                }
            }));
            System.out.printf("%s is ready to detect useless code within(%s..*)%n", MY_SIMPLE_NAME, packagePrefix);
        } finally {
            Thread.currentThread().setContextClassLoader(Thread.currentThread().getContextClassLoader().getParent());
        }
    }

    private static void installPrivateClassLoader() throws IOException {
        List<URL> urls = new ArrayList<URL>();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "duck");

        URL jarLocation = DuckAgent.class.getProtectionDomain().getCodeSource().getLocation();
        JarFile jarFile = new JarFile(jarLocation.getFile());
        for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
            JarEntry jarEntry = entries.nextElement();
            if (!jarEntry.isDirectory() && jarEntry.getName().matches("lib/.*\\.jar$")) {
                File externalJar = new File(tmpDir, jarEntry.getName());
                if (externalJar.isFile() && externalJar.length() == jarEntry.getSize()) {
                    System.out.printf("%s is already exported to %s%n", jarEntry, tmpDir);
                } else {
                    writeExternalJar(jarFile, jarEntry, externalJar);
                }
                urls.add(externalJar.toURI().toURL());
            }
        }

        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(urls.toArray(new URL[urls.size()]), parent));
    }

    private static void writeExternalJar(JarFile jarFile, JarEntry jarEntry, File externalJar) throws IOException {
        externalJar.getParentFile().mkdirs();
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = new BufferedInputStream(jarFile.getInputStream(jarEntry));
            outputStream = new BufferedOutputStream(new FileOutputStream(externalJar));

            byte buffer[] = new byte[1024];
            int len;

            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        System.out.printf("Created %s%n", externalJar);
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

    private static String parsePackagePrefix(String args) {
        Pattern pattern = Pattern.compile("packagePrefix=([\\w.]+)");
        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Usage: javaagent:/path/to/duck-agent.jar=packagePrefix=<package>");
        }
        return matcher.group(1);
    }
}
