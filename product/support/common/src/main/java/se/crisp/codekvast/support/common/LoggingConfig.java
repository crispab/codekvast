package se.crisp.codekvast.support.common;

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
public class LoggingConfig {

    public static final String CODEKVAST_LOG_PATH = "CODEKVAST_LOG_PATH";
    public static final String CODEKVAST_LOG_BASENAME = "CODEKVAST_LOG_BASENAME";
    public static final String CODEKVAST_LOG_CONSOLE_THRESHOLD = "CODEKVAST_LOG_CONSOLE_THRESHOLD";

    public static void configure(Class<?> mainClass, String appName) {
        File varLogCodekvast = new File("/var/log/codekvast");
        String appHome = System.getenv("APP_HOME");
        File appHomeLog = appHome == null ? null : new File(appHome, "log");
        String codePath = mainClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        String consoleThreshold = "OFF";

        boolean makeLogPathWritable = false;
        String logPath = System.getProperty("codekvast.logPath", System.getenv(CODEKVAST_LOG_PATH));
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
            consoleThreshold = "INFO";
        } else if (codePath.endsWith("/build/classes/main/")) {
            // Running from gradle run
            logPath = codePath.replace("/build/classes/main/", "/build/log");
            makeLogPathWritable = true;
            consoleThreshold = "INFO";
        } else if (codePath.endsWith("/build/classes/production/" + appName + "/")) {
            // Running from IDEA at $MODULE_DIR
            logPath = codePath.replace("/build/classes/production/" + appName + "/", "/build/log");
            makeLogPathWritable = true;
            consoleThreshold = "INFO";
        } else {
            logPath = ".";
            consoleThreshold = "INFO";
        }

        File resultDir = new File(logPath);
        if (makeLogPathWritable) {
            resultDir.mkdirs();
        }

        if (!resultDir.isDirectory()) {
            logPath = System.getProperty("user.dir");
            System.err.println(getCanonicalPath(resultDir) + " is not a directory, will log to working directory, which is " +
                                       getCanonicalPath(new File(logPath)));
            consoleThreshold = "INFO";
        }

        System.setProperty(CODEKVAST_LOG_PATH, logPath);
        System.setProperty(CODEKVAST_LOG_BASENAME, appName);
        System.setProperty(CODEKVAST_LOG_CONSOLE_THRESHOLD, consoleThreshold);
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
