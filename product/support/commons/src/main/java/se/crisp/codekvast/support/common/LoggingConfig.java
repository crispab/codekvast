/**
 * Copyright (c) 2015 Crisp AB
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
package se.crisp.codekvast.support.common;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;

/**
 * Finds out where the app is located and calculates an absolute path for the Logback log files.
 * <p>
 * Also sets LOG_CONSOLE_THRESHOLD if running from development environment.
 * <p>
 * See logback.xml
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
@UtilityClass
public class LoggingConfig {

    private static final String CODEKVAST_LOG_PATH = "codekvast.logPath";
    private static final String CODEKVAST_LOG_PATH_AS_ENVVAR = "CODEKVAST_LOGPATH";
    private static final String CODEKVAST_LOG_BASENAME = "codekvast.log.baseName";
    private static final String CODEKVAST_LOG_CONSOLE_THRESHOLD = "codekvast.log.consoleThreshold";
    private static final String CODEKVAST_LOG_CONSOLE_THRESHOLD_AS_ENVVAR = "CODEKVAST_LOG_CONSOLE_THRESHOLD";

    public static void configure(Class<?> mainClass, String appName) {
        File varLogCodekvast = new File("/var/log/codekvast");
        String appHome = System.getenv("APP_HOME");
        File appHomeLog = appHome == null ? null : new File(appHome, "log");
        String codePath = mainClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        String defaultConsoleThreshold = "OFF";

        boolean makeLogPathWritable = false;
        String logPath = System.getProperty(CODEKVAST_LOG_PATH, System.getenv(CODEKVAST_LOG_PATH_AS_ENVVAR));
        if (logPath != null) {
            // use it as it is
        } else if (varLogCodekvast.isDirectory()) {
            logPath = getCanonicalPath(varLogCodekvast);
        } else if (appHomeLog != null) {
            logPath = getCanonicalPath(appHomeLog);
            makeLogPathWritable = true;
        } else if (codePath.contains("/build/libs/") && codePath.endsWith(".jar!/")) {
            // Running from Gradle workspace with java -jar build/libs/xxx.jar
            int p = codePath.lastIndexOf("/build/libs");
            logPath = codePath.substring(0, p) + "/build/log";
            makeLogPathWritable = true;
            defaultConsoleThreshold = "INFO";
        } else if (codePath.endsWith("/build/classes/main/")) {
            // Running from gradle run
            logPath = codePath.replace("/build/classes/main/", "/build/log");
            makeLogPathWritable = true;
            defaultConsoleThreshold = "INFO";
        } else if (codePath.endsWith("/build/classes/production/" + appName + "/")) {
            // Running from IDEA at $MODULE_DIR
            logPath = codePath.replace("/build/classes/production/" + appName + "/", "/build/log");
            makeLogPathWritable = true;
            defaultConsoleThreshold = "INFO";
        } else {
            logPath = ".";
            defaultConsoleThreshold = "INFO";
        }

        File resultDir = new File(logPath);
        if (makeLogPathWritable) {
            resultDir.mkdirs();
        }

        if (!resultDir.isDirectory()) {
            logPath = System.getProperty("user.dir");
            System.err.println(getCanonicalPath(resultDir) + " is not a directory, will log to working directory, which is " +
                                       getCanonicalPath(new File(logPath)));
            defaultConsoleThreshold = "INFO";
        }

        System.setProperty(CODEKVAST_LOG_PATH, logPath);
        System.setProperty(CODEKVAST_LOG_BASENAME, appName);

        setConsoleThreshold(defaultConsoleThreshold);
    }

    private static void setConsoleThreshold(String defaultConsoleThreshold) {
        String specifiedConsoleThreshold =
                System.getProperty(CODEKVAST_LOG_CONSOLE_THRESHOLD, System.getenv(CODEKVAST_LOG_CONSOLE_THRESHOLD_AS_ENVVAR));
        String consoleThreshold = specifiedConsoleThreshold == null ? defaultConsoleThreshold : specifiedConsoleThreshold;
        System.setProperty(CODEKVAST_LOG_CONSOLE_THRESHOLD, consoleThreshold);
        System.out.println("Setting console log threshold to " + consoleThreshold);
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
