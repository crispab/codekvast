package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Value;
import lombok.experimental.Builder;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class AppId {
    private final long organisationId;
    private final long appId;
    private final long jvmId;
}
