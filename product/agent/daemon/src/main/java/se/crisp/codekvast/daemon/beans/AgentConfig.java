package se.crisp.codekvast.daemon.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.daemon_api.model.v1.Constraints;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.net.URI;

/**
 * Encapsulates the configuration of the codekvast-daemon.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@ConfigurationProperties(prefix = "codekvast")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {
    @NotNull
    private File dataPath;

    @NotNull
    private String apiAccessID;

    @NotNull
    private String apiAccessSecret;

    @NotNull
    private URI serverUri;

    @Min(1)
    private int serverUploadIntervalSeconds;

    @NotNull
    @Size(max = Constraints.MAX_CODEKVAST_VERSION_LENGTH)
    String daemonVersion;

    @NotNull
    @Size(max = Constraints.MAX_CODEKVAST_VCS_ID_LENGTH)
    String daemonVcsId;

    public String getDisplayVersion() {
        return daemonVersion + "-" + daemonVcsId;
    }
}
