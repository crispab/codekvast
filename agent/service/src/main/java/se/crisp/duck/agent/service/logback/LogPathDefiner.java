package se.crisp.duck.agent.service.logback;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * Finds out where the code base is located and calculates an absolute path for the Logback log file.
 * See logback.xml
 *
 * @author Olle Hallin
 */
public class LogPathDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {

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
        return path;
    }
}
