package se.crisp.duck.agent.util;

import lombok.Value;
import lombok.experimental.Builder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author Olle Hallin
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration"})
@Value
@Builder
public class Configuration {
    private final boolean verbose;
    private final String customerName;
    private final String appName;
    private final String environment;
    private final URI codeBaseUri;
    private final String packagePrefix;
    private final String aspectjOptions;
    private final File dataPath;
    private final int sensorDumpIntervalSeconds;
    private final int serverUploadIntervalSeconds;
    private final URI serverUri;

    public File getDataFile() {
        return new File(dataPath, "usage.dat");
    }

    public File getSignatureFile() {
        return new File(dataPath, "signatures.dat");
    }

    public File getSensorFile() {
        return new File(dataPath, "sensor.properties");
    }

    public File getAspectFile() {
        return new File(dataPath, "aop.xml");
    }

    public static Configuration parseConfigFile(String configFile) {
        File file = new File(configFile);

        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("Configuration file '%s' does not exist", file.getAbsolutePath()));
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException(String.format("'%s' is not a file", file.getAbsolutePath()));
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException(String.format("Cannot read configuration file '%s'", file.getAbsolutePath()));
        }

        try {
            Properties props = new Properties();
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            props.load(is);
            is.close();

            String customerName = getStringValue(props, "customerName");
            String appName = getStringValue(props, "appName");
            return Configuration.builder()
                                .customerName(customerName)
                                .appName(appName)
                                .environment(getStringValue(props, "environment"))
                                .codeBaseUri(getUriValue(props, "codeBaseUri"))
                                .packagePrefix(getStringValue(props, "packagePrefix"))
                                .aspectjOptions(props.getProperty("aspectjOptions", ""))
                                .sensorDumpIntervalSeconds(getIntValue(props, "sensorDumpIntervalSeconds", 600))
                                .dataPath(new File(props.getProperty("dataPath", getDefaultDataPath(customerName, appName))))
                                .serverUploadIntervalSeconds(getIntValue(props, "serverUploadIntervalSeconds", 3600))
                                .serverUri(getUriValue(props, "serverUri"))
                                .verbose(Boolean.parseBoolean(props.getProperty("verbose", "false")))
                                .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    private static String getDefaultDataPath(String customerName, String appName) {
        return System
                .getProperty("java.io.tmpdir") + File.separator
                + "duck" + File.separator
                + "agent" + File.separator
                + normalizePathName(customerName) + File.separator
                + normalizePathName(appName);
    }

    private static String normalizePathName(String path) {
        return path.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    private static URI getUriValue(Properties props, String key) {
        String value = getStringValue(props, key);
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Illegal URI value for %s: %s", key, value));
        }
    }

    private static int getIntValue(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("Illegal integer value for %s: %s", key, value));
            }
        }
        return defaultValue;
    }

    private static String getStringValue(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value;
    }
}
