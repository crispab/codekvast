package se.crisp.codekvast.shared.config;

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

    public static final String ENVVAR_CATALINA_BASE = "CATALINA_BASE";
    public static final String ENVVAR_CATALINA_HOME = "CATALINA_HOME";
    public static final String ENVVAR_CONFIG = "CODEKVAST_CONFIG";
    public static final String ENVVAR_HOME = "CODEKVAST_HOME";
    public static final String ENVVAR_VERBOSE = "CODEKVAST_VERBOSE";
    public static final String SYSPROP_CATALINA_BASE = "catalina.base";
    public static final String SYSPROP_CATALINA_HOME = "catalina.home";
    public static final String SYSPROP_CONFIG = "codekvast.configuration";
    public static final String SYSPROP_HOME = "codekvast.home";
    public static final String SYSPROP_OPTS = "codekvast.options";

    private CollectorConfigLocator() {
    }

    /**
     * Attempts to find a codekvast-collector.conf or codekvast.conf in a number of locations.
     *
     * @param out Print stream used for logging
     * @return null if no config file could be found.
     */
    public static URI locateConfig(PrintStream out) {
        boolean verbose = CollectorConfig.isSyspropVerbose();
        File file = tryLocation(out, verbose, System.getProperty(SYSPROP_CONFIG));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, System.getenv(ENVVAR_CONFIG));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getProperty(SYSPROP_HOME), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getenv(ENVVAR_HOME), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getProperty(SYSPROP_CATALINA_HOME), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getenv(ENVVAR_CATALINA_HOME), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getProperty(SYSPROP_CATALINA_BASE), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getenv(ENVVAR_CATALINA_BASE), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(getCollectorHome(), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, "/etc/codekvast");
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, "/etc");
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }
        printMessage(out, verbose, "No configuration file found, Codekvast will not start.");
        return null;
    }

    private static void printMessage(PrintStream out, boolean verbose, String message) {
        if (verbose) {
            out.println("Codekvast: " + message);
        }
    }

    private static String constructLocation(String home, String subdirectory) {
        return home == null ? null : new File(home, subdirectory).getAbsolutePath();
    }

    private static File tryLocation(PrintStream out, boolean verbose, String location) {
        if (location == null) {
            return null;
        }

        File file = new File(location);
        printMessage(out, verbose, "Examining " + file);
        if (file.isFile()) {
            return file;
        }

        file = new File(location, "codekvast-collector.conf");
        printMessage(out, verbose, "Looking for " + file);
        if (file.canRead()) {
            return file;
        }

        file = new File(location, "codekvast.conf");
        printMessage(out, verbose, "Looking for " + file);
        if (file.canRead()) {
            return file;
        }

        return null;
    }

    private static String getCollectorHome() {
        try {
            File myJar = new File(CollectorConfigLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File home = myJar.getParentFile();
            if (home.getName().endsWith("/lib")) {
                home = home.getParentFile();
            } else if (home.getName().endsWith("/endorsed")) {
                home = home.getParentFile();
            }
            return home.getAbsolutePath();
        } catch (URISyntaxException e) {
        }
        return null;
    }
}
