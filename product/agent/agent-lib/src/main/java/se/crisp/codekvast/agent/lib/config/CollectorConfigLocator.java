/**
 * Copyright (c) 2015, 2016 Crisp AB
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
package se.crisp.codekvast.agent.lib.config;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;

/**
 * This class locates the file to feed into {@link CollectorConfigFactory#parseCollectorConfig(URI, String)} .
 * 
 * It does this by checking a number of locations, stopping as soon as a file with the correct name is found.
 *
 * @author olle.hallin@crisp.se
 */
public class CollectorConfigLocator {

    private static final String ENVVAR_CATALINA_BASE = "CATALINA_BASE";
    private static final String ENVVAR_CATALINA_HOME = "CATALINA_HOME";
    private static final String ENVVAR_CONFIG = "CODEKVAST_CONFIG";
    private static final String ENVVAR_HOME = "CODEKVAST_HOME";
    public static final String ENVVAR_VERBOSE = "CODEKVAST_VERBOSE";
    private static final String SYSPROP_CATALINA_BASE = "catalina.base";
    private static final String SYSPROP_CATALINA_HOME = "catalina.home";
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
        boolean verbose = CollectorConfigFactory.isSyspropVerbose();
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

        file = tryLocation(out, verbose, constructLocation(System.getProperty(SYSPROP_HOME), ""));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getProperty(SYSPROP_HOME), "conf"));
        if (file != null) {
            printMessage(out, verbose, "Found " + file);
            return file.toURI();
        }

        file = tryLocation(out, verbose, constructLocation(System.getenv(ENVVAR_HOME), ""));
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
            File home = new File(CollectorConfigLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            String homeName = home.getName();
            if (homeName.endsWith("/lib") || homeName.endsWith("/endorsed") || homeName.endsWith("/javaagent")) {
                home = home.getParentFile();
            }
            return home.getAbsolutePath();
        } catch (Exception ignored) {
        }
        return null;
    }
}
