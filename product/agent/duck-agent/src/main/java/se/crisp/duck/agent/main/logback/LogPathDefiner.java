package se.crisp.duck.agent.main.logback;

import ch.qos.logback.core.PropertyDefinerBase;

import java.io.File;

/**
 * Finds out where the duck-agent is installed and calculates an absolute path for the Logback log file. See logback.xml
 *
 * @author Olle Hallin
 */
public class LogPathDefiner extends PropertyDefinerBase {

    public static final String LOG_PATH_PROPERTY = "duck.agentLogPath";

    @Override
    public String getPropertyValue() {
        String path = System.getProperty(LOG_PATH_PROPERTY);
        if (path == null) {
            // Not started with an explicit log file, compute the logical location for the log file...

            path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

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
        }
        File result = new File(path);
        result.mkdirs();
        return result.getAbsolutePath();
    }
}
