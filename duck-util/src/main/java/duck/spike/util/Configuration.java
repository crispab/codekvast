package duck.spike.util;

import java.io.File;

/**
 * @author Olle Hallin
 */
public class Configuration {
    private final File dataPath;
    private final String packagePrefix;
    private final int dumpIntervalSeconds;
    private final String appName;

    public Configuration() {
        dataPath = new File(System.getProperty("java.io.tmpdir"), "duck");
        packagePrefix = "se.crisp";
        dumpIntervalSeconds = 10;
        appName = "Crisp Duck Sample App";
    }

    public File getDataFile() {
        return new File(dataPath, "duck-usage.txt");
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public int getDumpIntervalSeconds() {
        return dumpIntervalSeconds;
    }

    public String getAppName() {
        return appName;
    }

    public static Configuration parseConfigFile(String configFile) {
        // TODO: read properties from configFile

        return new Configuration();
    }
}
