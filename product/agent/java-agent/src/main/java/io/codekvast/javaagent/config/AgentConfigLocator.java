/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.javaagent.config;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import lombok.extern.java.Log;

/**
 * This class locates the file to feed into {@link AgentConfigFactory#parseAgentConfig(File,
 * String)} .
 *
 * <p>It does this by checking a number of locations, stopping as soon as a file with the correct
 * name is found.
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

  private AgentConfigLocator() {}

  /**
   * Attempts to find codekvast.conf in a number of locations.
   *
   * @return null if no config file could be found.
   */
  public static File locateConfig() {

    List<String> explicitLocations =
        Arrays.asList(System.getProperty(SYSPROP_CONFIG), System.getenv(ENVVAR_CODEKVAST_CONFIG));
    for (String location : explicitLocations) {
      if (location != null) {
        File file = tryLocation(location);
        if (file != null) {
          return file;
        }

        // Do not look in automatic locations if some explicit location was given.
        logger.warning(
            String.format(
                "Invalid value of -D%s or %s: %s, Codekvast will not start.",
                SYSPROP_CONFIG, ENVVAR_CODEKVAST_CONFIG, location));
        return null;
      }
    }

    List<String> automaticLocations =
        Arrays.asList(
            "./codekvast.conf",
            "./conf/codekvast.conf",
            constructLocation(System.getProperty(SYSPROP_CATALINA_HOME), "conf"),
            constructLocation(System.getenv(ENVVAR_CATALINA_HOME), "conf"),
            constructLocation(System.getProperty(SYSPROP_CATALINA_BASE), "conf"),
            constructLocation(System.getenv(ENVVAR_CATALINA_BASE), "conf"),
            constructLocation(System.getenv(ENVVAR_HOME), ".config"),
            "/etc/codekvast/codekvast.conf",
            "/etc/codekvast.conf");

    for (String location : automaticLocations) {
      if (location != null) {
        File file = tryLocation(location);
        if (file != null) {
          return file;
        }
      }
    }

    logger.warning("No configuration file found, Codekvast will not start.");
    return null;
  }

  private static String constructLocation(String dir, String subdirectory) {
    return dir == null
        ? null
        : new File(new File(dir, subdirectory), "codekvast.conf").getAbsolutePath();
  }

  private static File tryLocation(String location) {
    if (location == null) {
      return null;
    }

    File file = new File(location);
    logger.fine("Trying " + file);
    if (file.isFile() && file.canRead()) {
      logger.info("Found " + file);
      return file;
    }

    return null;
  }
}
