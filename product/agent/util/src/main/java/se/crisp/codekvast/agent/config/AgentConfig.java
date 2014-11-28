package se.crisp.codekvast.agent.config;

import lombok.*;
import lombok.experimental.Builder;
import se.crisp.codekvast.agent.util.ConfigUtils;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Encapsulates the configuration that is shared between codekvast-agent and codekvast-collector.
 *
 * It also contains methods for reading and writing agent configuration files.
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentConfig implements CodekvastConfig {
    public static final int DEFAULT_UPLOAD_INTERVAL_SECONDS = 3600;
    public static final String DEFAULT_API_ACCESS_ID = "agent";
    public static final String DEFAULT_API_ACCESS_SECRET = "0000";

    @NonNull
    private final SharedConfig sharedConfig;
    @NonNull
    private final String apiAccessID;
    @NonNull
    private final String apiAccessSecret;
    @NonNull
    private final String environment;
    @NonNull
    private final URI serverUri;
    private final int serverUploadIntervalSeconds;

    public int getServerUploadIntervalMillis() {
        return serverUploadIntervalSeconds * 1000;
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
                              .apiAccessID(ConfigUtils.getOptionalStringValue(props, "apiAccessID", DEFAULT_API_ACCESS_ID))
                              .apiAccessSecret(ConfigUtils.getOptionalStringValue(props, "apiAccessSecret", DEFAULT_API_ACCESS_SECRET))
                              .environment(ConfigUtils.getMandatoryStringValue(props, "environment"))
                              .serverUploadIntervalSeconds(ConfigUtils.getOptionalIntValue(props, "serverUploadIntervalSeconds",
                                                                                           DEFAULT_UPLOAD_INTERVAL_SECONDS))
                              .serverUri(ConfigUtils.getMandatoryUriValue(props, "serverUri", true))
                              .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", uri, e.getMessage()), e);
        }
    }

    @SneakyThrows(URISyntaxException.class)
    public static AgentConfig createSampleAgentConfig() {
        return AgentConfig.builder()
                          .sharedConfig(SharedConfig.buildSampleSharedConfig())
                          .apiAccessID(DEFAULT_API_ACCESS_ID)
                          .apiAccessSecret(DEFAULT_API_ACCESS_SECRET)
                          .environment("environment")
                          .serverUploadIntervalSeconds(DEFAULT_UPLOAD_INTERVAL_SECONDS)
                          .serverUri(new URI("http://localhost:8090"))
                          .build();
    }

}
