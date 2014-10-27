package se.crisp.codekvast.agent.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Utility class for config stuff.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
public final class ConfigUtils {

    public static final String SAMPLE_DATA_PATH = "/var/lib/codekvast";

    private ConfigUtils() {
    }

    public static String getNormalizedPackagePrefix(String packagePrefix) {
        int dot = packagePrefix.length() - 1;
        while (dot >= 0 && packagePrefix.charAt(dot) == '.') {
            dot -= 1;
        }
        return packagePrefix.substring(0, dot + 1);
    }


    public static String getOptionalStringValue(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static String getNormalizedChildPath(String customerName, String appName) {
        return normalizePathName(customerName) + File.separator + normalizePathName(appName);
    }

    public static String normalizePathName(String path) {
        return path.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
    }

    public static int getOptionalIntValue(Properties props, String key, int defaultValue) {
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

    public static String getMandatoryStringValue(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value;
    }

    public static URI getMandatoryUriValue(Properties props, String key, boolean removeTrailingSlash) {
        String value = getMandatoryStringValue(props, key);
        if (removeTrailingSlash && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("Illegal URI value for %s: %s", key, value));
        }
    }
}
