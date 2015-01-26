package se.crisp.codekvast.server.codekvast_server.model;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class Application {
    @NonNull
    private final AppId appId;

    @NonNull
    @NotBlank
    private final String name;
}
