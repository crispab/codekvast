package se.crisp.codekvast.agent.util;

import lombok.*;
import lombok.experimental.Builder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Encapsulates the configuration that is shared between codekvast-agent and codekvast-collector.
 * <p>
 * It also contains methods for reading and writing agent configuration files.
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentConfig {
    public static final int DEFAULT_UPLOAD_INTERVAL_SECONDS = 3600;
    public static final String DEFAULT_API_PASSWORD = "0000";
    public static final String DEFAULT_API_USERNAME = "agent";
    public static final String UNSPECIFIED_VERSION = "unspecified";

    @NonNull
    private final SharedConfig sharedConfig;

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
    private final File dataPath;
    @NonNull
    private final URI serverUri;
    @NonNull
    private final String apiUsername;
    @NonNull
    private final String apiPassword;
    private final int serverUploadIntervalSeconds;

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
        return super.getUsageFile();
    }

    private File myDataPath() {
        return new File(dataPath, ConfigUtils.getNormalizedChildPath(customerName, appName));
    }

    public File getJvmRunFile() {
        return super.getJvmRunFile();
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
                              .customerName(ConfigUtils.getMandatoryStringValue(props, "customerName"))
                              .appName(ConfigUtils.getMandatoryStringValue(props, "appName"))
                              .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .environment(ConfigUtils.getMandatoryStringValue(props, "environment"))
                              .codeBaseUri(ConfigUtils.getMandatoryUriValue(props, "codeBaseUri", false))
                              .packagePrefix(ConfigUtils.getMandatoryStringValue(props, "packagePrefix"))
                              .dataPath(new File(ConfigUtils.getMandatoryStringValue(props, "dataPath")))
                              .serverUploadIntervalSeconds(ConfigUtils.getOptionalIntValue(props, "serverUploadIntervalSeconds",
                                                                               DEFAULT_UPLOAD_INTERVAL_SECONDS))
                              .serverUri(ConfigUtils.getMandatoryUriValue(props, "serverUri", true))
                              .apiUsername(ConfigUtils.getOptionalStringValue(props, "apiUsername", DEFAULT_API_USERNAME))
                              .apiPassword(ConfigUtils.getOptionalStringValue(props, "apiPassword", DEFAULT_API_PASSWORD))
                              .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", uri, e.getMessage()), e);
        }
    }

    @SneakyThrows(URISyntaxException.class)
    public static AgentConfig createSampleConfiguration() {
        return AgentConfig.builder()
                          .customerName("Customer Name")
                          .appName("app-name")
                          .appVersion("application version")
                          .environment("environment")
                          .packagePrefix("com.acme")
                          .codeBaseUri(new URI("file:/path/to/my/code/base"))
                          .dataPath(new File(ConfigUtils.SAMPLE_DATA_PATH))
                          .serverUploadIntervalSeconds(DEFAULT_UPLOAD_INTERVAL_SECONDS)
                          .serverUri(new URI("http://localhost:8080"))
                          .apiUsername(DEFAULT_API_USERNAME)
                          .apiPassword(DEFAULT_API_PASSWORD)
                          .build();
    }

}
