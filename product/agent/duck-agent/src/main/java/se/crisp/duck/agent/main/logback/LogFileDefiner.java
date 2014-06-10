package se.crisp.duck.agent.main.logback;

import ch.qos.logback.core.PropertyDefinerBase;

import java.io.File;

/**
 * Finds out where the code base is located and calculates an absolute path for the Logback log file.
 * See logback.xml
 *
 * @author Olle Hallin
 */
public class LogFileDefiner extends PropertyDefinerBase {

    public static final String LOG_FILE_PROPERTY = "duck.agentLogFile";

    @Override
    public String getPropertyValue() {
        String result = System.getProperty(LOG_FILE_PROPERTY);
        if (result == null) {
            // Not started with an explicit log file, compute the logical location for the log file...

            String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

            if (path.endsWith(".jar")) {
                // Running from start script
                path = path.substring(0, path.lastIndexOf("/")).replace("/lib", "/log");
            }

            if (path.endsWith("/build/classes/main/")) {
                // Running from gradle run
                path = path.replace("/build/classes/main/", "/build");
            }

            if (path.endsWith("/build/classes/production/duck-agent/")) {
                // Running from IDEA at $MODULE_DIR
                path = path.replace("/build/classes/production/duck-agent/", "/duck-agent/build");
            }
            result = path + File.separator + "duck-agent.log";
        }
        return result;
    }
}
