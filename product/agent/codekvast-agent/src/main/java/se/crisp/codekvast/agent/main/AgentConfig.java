package se.crisp.codekvast.agent.main;

import lombok.AllArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;

/**
 * Encapsulates the configuration of the codekvast-agent.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@ConfigurationProperties(prefix = "codekvast")
@Builder
@AllArgsConstructor
public class AgentConfig {
    private File dataPath;
    private String apiAccessID;
    private String apiAccessSecret;
    private URI serverUri;
    private int serverUploadIntervalSeconds;

    public AgentConfig() {
        int i = 17;
    }

    public File getDataPath() {
        return dataPath;
    }

    public void setDataPath(File dataPath) {
        this.dataPath = dataPath;
    }

    public String getApiAccessID() {
        return apiAccessID;
    }

    public void setApiAccessID(String apiAccessID) {
        this.apiAccessID = apiAccessID;
    }

    public String getApiAccessSecret() {
        return apiAccessSecret;
    }

    public void setApiAccessSecret(String apiAccessSecret) {
        this.apiAccessSecret = apiAccessSecret;
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void setServerUri(URI serverUri) {
        this.serverUri = serverUri;
    }

    public int getServerUploadIntervalSeconds() {
        return serverUploadIntervalSeconds;
    }

    public void setServerUploadIntervalSeconds(int serverUploadIntervalSeconds) {
        this.serverUploadIntervalSeconds = serverUploadIntervalSeconds;
    }
}
