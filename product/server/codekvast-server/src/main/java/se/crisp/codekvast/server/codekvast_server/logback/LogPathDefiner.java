package se.crisp.codekvast.server.codekvast_server.logback;

import ch.qos.logback.core.PropertyDefinerBase;

import java.io.File;
import java.io.IOException;

/**
 * Finds out where the app is located and calculates an absolute path for the Logback log files. See logback.xml
 *
 * @author olle.hallin@crisp.se
 */
public class LogPathDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String result = System.getProperty("codekvast.logPath");
        boolean makeResultWritable = false;
        if (result != null) {
            // use it as it is
        } else if (path.endsWith(".jar")) {
            // Running from application start script
            result = path.substring(0, path.lastIndexOf("/")).replace("/lib", "/log");
            makeResultWritable = true;
        } else if (path.contains("/build/libs/") && path.endsWith(".jar!/")) {
            // Running from Gradle workspace with java -jar build/libs/xxx.jar
            int p = path.lastIndexOf("/build/libs");
            result = path.substring(0, p) + "/build";
        } else if (path.endsWith(".jar!/")) {
            // Running from java -jar outside the Gradle workspace
            result = "/var/log/codekvast";
        } else if (path.endsWith("/build/classes/main/")) {
            // Running from gradle run
            result = path.replace("/build/classes/main/", "/build");
        } else if (path.endsWith("/build/classes/production/codekvast-server/")) {
            // Running from IDEA at $MODULE_DIR
            result = path.replace("/build/classes/production/codekvast-server/", "/server/codekvast-server/build");
        } else {
            result = ".";
        }

        File resultDir = new File(result);
        if (makeResultWritable) {
            resultDir.mkdirs();
        }

        if (!resultDir.isDirectory() || !resultDir.canWrite()) {
            result = System.getProperty("user.dir");
            System.err.println(getCanonicalPath(resultDir) + " is not writable, will log to working directory, which is " +
                                       getCanonicalPath(new File(result)));
        }

        return result;
    }

    private String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
