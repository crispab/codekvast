package se.crisp.codekvast.server.agent_api;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.net.URI;

/**
 * Configuration data for how to communicate with the server.
 *
 * @author Olle Hallin
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
