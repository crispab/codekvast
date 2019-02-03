/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.javaagent.config;

import lombok.extern.java.Log;

import java.io.File;

/**
 * This class locates the file to feed into {@link AgentConfigFactory#parseAgentConfig(File, String)} .
 * 
 * It does this by checking a number of locations, stopping as soon as a file with the correct name is found.
 *
 * @author olle.hallin@crisp.se
 */
@Log
public class AgentConfigLocator {

    private static final String ENVVAR_CATALINA_BASE = "CATALINA_BASE";
    private static final String ENVVAR_CATALINA_HOME = "CATALINA_HOME";
    private static final String ENVVAR_CODEKVAST_CONFIG = "CODEKVAST_CONFIG";
    private static final String ENVVAR_HOME = "HOME";
    private static final String SYSPROP_CATALINA_BASE = "catalina.base";
    private static final String SYSPROP_CATALINA_HOME = "catalina.home";
    static final String SYSPROP_CONFIG = "codekvast.configuration";

    private AgentConfigLocator() {
    }

    /**
     * Attempts to find codekvast.conf in a number of locations.
     *
     * @return null if no config file could be found.
     */
    public static File locateConfig() {
        File file = tryLocation(System.getProperty(SYSPROP_CONFIG));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(System.getenv(ENVVAR_CODEKVAST_CONFIG));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(".");
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation("./conf");
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(constructLocation(System.getProperty(SYSPROP_CATALINA_HOME), "conf"));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(constructLocation(System.getenv(ENVVAR_CATALINA_HOME), "conf"));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(constructLocation(System.getProperty(SYSPROP_CATALINA_BASE), "conf"));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(constructLocation(System.getenv(ENVVAR_CATALINA_BASE), "conf"));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation(constructLocation(System.getenv(ENVVAR_HOME), ".config"));
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation("/etc/codekvast");
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }

        file = tryLocation("/etc");
        if (file != null) {
            logger.info("Found " + file);
            return file;
        }
        logger.warning("No configuration file found, Codekvast will not start.");
        return null;
    }

    private static String constructLocation(String home, String subdirectory) {
        return home == null ? null : new File(home, subdirectory).getAbsolutePath();
    }

    private static File tryLocation(String location) {
        if (location == null) {
            return null;
        }

        File file = new File(location);
        logger.fine("Examining " + file);
        if (file.isFile()) {
            return file;
        }

        file = new File(location, "codekvast.conf");
        logger.fine("Looking for " + file);
        if (file.canRead()) {
            return file;
        }

        return null;
    }
}
