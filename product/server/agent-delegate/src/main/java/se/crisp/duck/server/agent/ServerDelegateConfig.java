package se.crisp.duck.server.agent;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.net.URI;

/**
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
}
