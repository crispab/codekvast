package se.crisp.codekvast.agent.main;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;

/**
 * Encapsulates the configuration of the codekvast-agent.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@ConfigurationProperties(prefix = "codekvast")
@Data
@Builder
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
    String version;

    @NotNull
    String vcsId;
}
