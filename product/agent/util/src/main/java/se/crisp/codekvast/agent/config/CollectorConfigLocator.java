package se.crisp.codekvast.agent.config;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class locates the file to feed into {@link CollectorConfig#parseCollectorConfig(URI, String)} .
 * 
 * It does this by checking a number of locations, stopping as soon as a file with the correct name is found.
 *
 * @author olle.hallin@crisp.se
 */
public class CollectorConfigLocator {

    public static final String ENVVAR_CONFIG = "CODEKVAST_CONFIG";
    public static final String ENVVAR_HOME = "CODEKVAST_HOME";
    public static final String SYSPROP_CONFIG = "codekvast.configuration";
    public static final String SYSPROP_OPTS = "codekvast.options";
    public static final String SYSPROP_HOME = "codekvast.home";
    private static final String SYSPROP_CATALINA_HOME = "catalina.home";

    private CollectorConfigLocator() {
    }

    public static URI locateConfig(PrintStream out) {
        File file = tryLocation(out, System.getenv(ENVVAR_CONFIG));
        if (file != null) {
            return file.toURI();
        }

        file = tryLocation(out, System.getProperty(SYSPROP_CONFIG));
        if (file != null) {
            return file.toURI();
        }

        file = tryLocation(out, constructLocation(System.getenv(ENVVAR_HOME), "conf"));
        if (file != null) {
            return file.toURI();
        }

        file = tryLocation(out, constructLocation(System.getProperty(SYSPROP_HOME), "conf"));
        if (file != null) {
            return file.toURI();
        }

        file = tryLocation(out, constructLocation(System.getProperty(SYSPROP_CATALINA_HOME), "conf"));
        if (file != null) {
            return file.toURI();
        }

        file = tryLocation(out, constructLocation(getCollectorHome(), "conf"));
        if (file != null) {
            return file.toURI();
        }
        throw new IllegalArgumentException("Cannot find codekvast.conf");
    }

    private static String constructLocation(String home, String subdirectory) {
        return home == null ? null : new File(home, subdirectory).getAbsolutePath();
    }

    private static File tryLocation(PrintStream out, String location) {
        if (location == null) {
            return null;
        }

        File file = new File(location);
        if (file.isFile()) {
            out.println("Found " + file);
            return file;
        }

        file = new File(location, "codekvast-collector.conf");
        if (file.canRead()) {
            out.println("Found " + file);
            return file;
        }

        file = new File(location, "codekvast.conf");
        if (file.canRead()) {
            out.println("Found " + file);
            return file;
        }

        out.println("No codekvast.conf in " + location);
        return null;
    }

    private static String getCollectorHome() {
        try {
            File myJar = new File(CollectorConfigLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File home = myJar.getParentFile();
            if (home.getName().endsWith("/lib")) {
                home = home.getParentFile();
            }
            return home.getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        return null;
    }
}
