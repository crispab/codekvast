package se.crisp.duck.agent.util;

import lombok.*;
import lombok.experimental.Builder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Encapsulates the configuration that is shared between duck-agent and duck-sensor.
 * <p/>
 * It also contains methods for reading and writing agent configuration files.
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentConfig {
    public static final int DEFAULT_SENSOR_RESOLUTION_INTERVAL_SECONDS = 600;
    public static final int DEFAULT_UPLOAD_INTERVAL_MILLIS = 3600000;
    public static final String DEFAULT_ASPECTJ_OPTIONS = "";
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    public static final boolean DEFAULT_VERBOSE = false;
    public static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    public static final String UNSPECIFIED_VERSION = "unspecified";

    @NonNull
    private final String customerName;
    @NonNull
    private final String appName;
    @NonNull
    private final String appVersion;
    @NonNull
    private final String environment;
    @NonNull
    private final String codeBaseName;
    @NonNull
    private final URI codeBaseUri;
    @NonNull
    private final String packagePrefix;
    @NonNull
    private final String aspectjOptions;
    @NonNull
    private final File dataPath;
    @NonNull
    private final URI serverUri;
    private final int sensorResolutionIntervalSeconds;
    private final int serverUploadIntervalMillis;
    private final boolean clobberAopXml;
    private final boolean verbose;

    public File getUsageFile() {
        return new File(dataPath, "usage.dat");
    }

    public File getSensorFile() {
        return new File(dataPath, "sensor.dat");
    }

    public File getSignatureFile() {
        return new File(dataPath, "signatures.dat");
    }

    public File getAspectFile() {
        return new File(dataPath, "aop.xml");
    }

    public File getAgentLogFile() {
        return new File(dataPath, "duck-agent.log");
    }

    public File getSensorLogFile() {
        return new File(dataPath, "duck-sensor.log");
    }

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Duck AgentConfig");
    }

    public static AgentConfig parseConfigFile(String file) {
        return parseConfigFile(new File(file).toURI());
    }

    public static AgentConfig parseConfigFile(URI uri) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);
            String customerName = getMandatoryStringValue(props, "customerName");
            String appName = getMandatoryStringValue(props, "appName");

            return AgentConfig.builder()
                              .customerName(customerName)
                              .appName(appName)
                              .appVersion(getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .environment(getMandatoryStringValue(props, "environment"))
                              .codeBaseName(getOptionalStringValue(props, "codeBaseName", customerName))
                              .codeBaseUri(getMandatoryUriValue(props, "codeBaseUri", false))
                              .packagePrefix(getMandatoryStringValue(props, "packagePrefix"))
                              .aspectjOptions(getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .sensorResolutionIntervalSeconds(getOptionalIntValue(props, "sensorResolutionIntervalSeconds",
                                                                                   DEFAULT_SENSOR_RESOLUTION_INTERVAL_SECONDS))
                              .dataPath(new File(getOptionalStringValue(props, "dataPath", getDefaultDataPath(customerName, appName))))
                              .serverUploadIntervalMillis(getOptionalIntValue(props, "serverUploadIntervalMillis",
                                                                              DEFAULT_UPLOAD_INTERVAL_MILLIS))
                              .serverUri(getMandatoryUriValue(props, "serverUri", true))
                              .verbose(Boolean.parseBoolean(getOptionalStringValue(props, "verbose", Boolean.toString(DEFAULT_VERBOSE))))
                              .clobberAopXml(Boolean.parseBoolean(getOptionalStringValue(props, "clobberAopXml",
                                                                                         Boolean.toString(DEFAULT_CLOBBER_AOP_XML))))
                              .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", uri, e.getMessage()), e);
        }
    }

    @SneakyThrows(URISyntaxException.class)
    public static AgentConfig createSampleConfiguration() {
        String customerName = "Customer Name";
        String appName = "app-name";
        return AgentConfig.builder()
                          .customerName(customerName)
                          .appName(appName)
                          .appVersion("application version")
                          .environment("environment")
                          .packagePrefix("com.acme")
                          .codeBaseName("Optional: my-code-base-name")
                          .codeBaseUri(new URI("file:/path/to/my/code/base"))
                          .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                          .dataPath(new File("/var/lib", getDataChildPath(customerName, appName)))
                          .sensorResolutionIntervalSeconds(DEFAULT_SENSOR_RESOLUTION_INTERVAL_SECONDS)
                          .serverUploadIntervalMillis(DEFAULT_UPLOAD_INTERVAL_MILLIS)
                          .serverUri(new URI("http://some-duck-server"))
                          .verbose(DEFAULT_VERBOSE)
                          .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                          .build();
    }

    private static String getOptionalStringValue(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    private static String getDefaultDataPath(String customerName, String appName) {
        File basePath = new File("/var/lib");
        if (!basePath.canWrite()) {
            basePath = new File(System.getProperty("java.io.tmpdir"));
        }
        return new File(basePath, getDataChildPath(customerName, appName)).getAbsolutePath();
    }

    private static String getDataChildPath(String customerName, String appName) {
        return "duck/" + normalizePathName(customerName) + "/" + normalizePathName(appName);
    }

    private static String normalizePathName(String path) {
        return path.replaceAll("[^a-zA-Z0-9_\\-]", "").toLowerCase();
    }

    private static URI getMandatoryUriValue(Properties props, String key, boolean removeTrailingSlash) {
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

    private static int getOptionalIntValue(Properties props, String key, int defaultValue) {
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

    private static String getMandatoryStringValue(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        return value;
    }

}
