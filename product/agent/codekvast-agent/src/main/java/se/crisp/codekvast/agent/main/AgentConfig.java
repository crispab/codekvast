package se.crisp.codekvast.agent.main;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentConfig {
    private File dataPath;
    private String apiAccessID;
    private String apiAccessSecret;
    private URI serverUri;
    private int serverUploadIntervalSeconds;

}
