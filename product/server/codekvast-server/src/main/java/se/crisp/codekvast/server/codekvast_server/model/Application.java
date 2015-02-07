package se.crisp.codekvast.server.codekvast_server.model;

import lombok.NonNull;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author olle.hallin@crisp.se
 */
@Value
public class Application {
    @NonNull
    private final AppId appId;

    @NonNull
    @NotBlank
    private final String name;
}
