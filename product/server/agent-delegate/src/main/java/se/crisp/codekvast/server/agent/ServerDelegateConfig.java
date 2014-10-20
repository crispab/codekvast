package se.crisp.codekvast.server.agent;

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
public class ServerDelegateConfig {

    @NonNull
    private final String customerName;

    @NonNull
    private final String appName;

    @NonNull
    private final String appVersion;

    @NonNull
    private final String codeBaseName;

    @NonNull
    private final String environment;

    @NonNull
    private final URI serverUri;

    @NonNull
    private final String apiUsername;

    @NonNull
    private final String apiPassword;
}
