package se.crisp.codekvast.server.daemon_api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.net.URI;

/**
 * Configuration data for how to communicate with the server.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class AgentApiConfig {

    @NonNull
    private final URI serverUri;

    @NonNull
    private final String apiAccessID;

    @NonNull
    private final String apiAccessSecret;
}
