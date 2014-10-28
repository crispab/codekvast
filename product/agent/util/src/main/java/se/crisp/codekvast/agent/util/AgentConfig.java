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
    public static final int DEFAULT_UPLOAD_INTERVAL_SECONDS = 3600;
    public static final String DEFAULT_API_PASSWORD = "0000";
    public static final String DEFAULT_API_USERNAME = "agent";

    @NonNull
    private final SharedConfig sharedConfig;
    @NonNull
    private final String environment;
    @NonNull
    private final URI serverUri;
    @NonNull
    private final String apiUsername;
    @NonNull
    private final String apiPassword;
    private final int serverUploadIntervalSeconds;

    public int getServerUploadIntervalMillis() {
        return serverUploadIntervalSeconds * 1000;
    }

    public File getSignatureFile() {
        return new File(sharedConfig.myDataPath(), "signatures.dat");
    }

    public File getAgentLogFile() {
        return new File(sharedConfig.myDataPath(), "codekvast-agent.log");
    }

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Codekvast AgentConfig");
    }

    public static AgentConfig parseAgentConfigFile(String file) {
        return parseAgentConfigFile(new File(file).toURI());
    }

    public static AgentConfig parseAgentConfigFile(URI uri) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            return AgentConfig.builder()
                              .sharedConfig(SharedConfig.buildSharedConfig(props))
                              .environment(ConfigUtils.getMandatoryStringValue(props, "environment"))
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
    public static AgentConfig createSampleAgentConfig() {
        return AgentConfig.builder()
                          .sharedConfig(SharedConfig.buildSampleSharedConfig())
                          .environment("environment")
                          .serverUploadIntervalSeconds(DEFAULT_UPLOAD_INTERVAL_SECONDS)
                          .serverUri(new URI("http://localhost:8080"))
                          .apiUsername(DEFAULT_API_USERNAME)
                          .apiPassword(DEFAULT_API_PASSWORD)
                          .build();
    }

}
