package se.crisp.codekvast.server.codekvast_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author olle.hallin@crisp.se
 */
@Data
@Component
@ConfigurationProperties(prefix = "codekvast")
public class CodekvastProperties {
    private boolean multiTenant = false;
    private int trulyDeadAfterHours = 30 * 24;
}
