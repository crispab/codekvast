package se.crisp.codekvast.agent.util;

import lombok.*;
import lombok.experimental.Builder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Encapsulates the configuration that is shared between codekvast-agent and codekvast-collector.
 * <p/>
 * It also contains methods for reading and writing agent configuration files.
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentConfig {
    public static final boolean DEFAULT_INVOKE_ASPECTJ_WEAVER = true;
    public static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    public static final boolean DEFAULT_VERBOSE = false;
    public static final int DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS = 600;
    public static final int DEFAULT_UPLOAD_INTERVAL_SECONDS = 3600;
    public static final String DEFAULT_API_PASSWORD = "0000";
    public static final String DEFAULT_API_USERNAME = "agent";
    public static final String DEFAULT_ASPECTJ_OPTIONS = "";
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
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
    private final URI codeBaseUri;
    @NonNull
    private final String packagePrefix;
    @NonNull
    private final String aspectjOptions;
    @NonNull
    private final File dataPath;
    @NonNull
    private final URI serverUri;
    @NonNull
    private final String apiUsername;
    @NonNull
    private final String apiPassword;
    private final int collectorResolutionSeconds;
    private final int serverUploadIntervalSeconds;
    private final boolean clobberAopXml;
    private final boolean verbose;
    private final boolean invokeAspectjWeaver;

    public String getNormalizedPackagePrefix() {
        int dot = packagePrefix.length() - 1;
        while (dot >= 0 && packagePrefix.charAt(dot) == '.') {
            dot -= 1;
        }
        return packagePrefix.substring(0, dot + 1);
    }

    public int getServerUploadIntervalMillis() {
        return serverUploadIntervalSeconds * 1000;
    }

    public File getUsageFile() {
        return new File(myDataPath(), "usage.dat");
    }

    private File myDataPath() {
        return new File(dataPath, getNormalizedChildPath(customerName, appName));
    }

    public File getJvmRunFile() {
        return new File(myDataPath(), "jvm-run.dat");
    }

    public File getSignatureFile() {
        return new File(myDataPath(), "signatures.dat");
    }

    public File getAspectFile() {
        return new File(myDataPath(), "aop.xml");
    }

    public File getAgentLogFile() {
        return new File(myDataPath(), "codekvast-agent.log");
    }

    public File getCollectorLogFile() {
        return new File(myDataPath(), "codekvast-collector.log");
    }

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Codekvast AgentConfig");
    }

    public static AgentConfig parseConfigFile(String file) {
        return parseConfigFile(new File(file).toURI());
    }

    public static AgentConfig parseConfigFile(URI uri) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            return AgentConfig.builder()
                              .customerName(getMandatoryStringValue(props, "customerName"))
                              .appName(getMandatoryStringValue(props, "appName"))
                              .appVersion(getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .environment(getMandatoryStringValue(props, "environment"))
                              .codeBaseUri(getMandatoryUriValue(props, "codeBaseUri", false))
                              .packagePrefix(getMandatoryStringValue(props, "packagePrefix"))
                              .aspectjOptions(getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .collectorResolutionSeconds(getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                              DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS))
                              .dataPath(new File(getOptionalStringValue(props, "dataPath", getDefaultDataPath())))
                              .serverUploadIntervalSeconds(getOptionalIntValue(props, "serverUploadIntervalSeconds",
                                                                               DEFAULT_UPLOAD_INTERVAL_SECONDS))
                              .serverUri(getMandatoryUriValue(props, "serverUri", true))
                              .apiUsername(getOptionalStringValue(props, "apiUsername", DEFAULT_API_USERNAME))
                              .apiPassword(getOptionalStringValue(props, "apiPassword", DEFAULT_API_PASSWORD))
                              .verbose(Boolean.parseBoolean(getOptionalStringValue(props, "verbose", Boolean.toString(DEFAULT_VERBOSE))))
                              .clobberAopXml(Boolean.parseBoolean(getOptionalStringValue(props, "clobberAopXml",
                                                                                         Boolean.toString(DEFAULT_CLOBBER_AOP_XML))))
                              .invokeAspectjWeaver(Boolean.parseBoolean(getOptionalStringValue(props, "invokeAspectjWeaver",
                                                                                               Boolean.toString
                                                                                                       (DEFAULT_INVOKE_ASPECTJ_WEAVER))))
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
                          .codeBaseUri(new URI("file:/path/to/my/code/base"))
                          .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                          .dataPath(new File("/var/lib"))
                          .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS)
                          .serverUploadIntervalSeconds(DEFAULT_UPLOAD_INTERVAL_SECONDS)
                          .serverUri(new URI("http://localhost:8080"))
                          .apiUsername(DEFAULT_API_USERNAME)
                          .apiPassword(DEFAULT_API_PASSWORD)
                          .verbose(DEFAULT_VERBOSE)
                          .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                          .invokeAspectjWeaver(DEFAULT_INVOKE_ASPECTJ_WEAVER)
                          .build();
    }

    private static String getOptionalStringValue(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    private static String getDefaultDataPath() {
        File basePath = new File("/var/lib");
        if (!basePath.canWrite()) {
            basePath = new File(System.getProperty("java.io.tmpdir"));
        }
        if (!basePath.canWrite()) {
            basePath = new File(".");
        }
        return basePath.getAbsolutePath();
    }

    private static String getNormalizedChildPath(String customerName, String appName) {
        return "codekvast/" + normalizePathName(customerName) + "/" + normalizePathName(appName);
    }

    private static String normalizePathName(String path) {
        return path.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
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
