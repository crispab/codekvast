package se.crisp.codekvast.server.codekvast_server.model;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class Application {
    @NonNull
    private final AppId appId;
    @NonNull
    private final String name;
    @NonNull
    private final String version;
    @NonNull
    private final String environment;
}
