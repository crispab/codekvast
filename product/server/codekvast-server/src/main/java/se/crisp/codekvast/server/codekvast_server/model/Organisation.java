package se.crisp.codekvast.server.codekvast_server.model;

import lombok.NonNull;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author olle.hallin@crisp.se
 */
@Value
public class Organisation {
    private final long id;

    @NonNull
    @NotBlank
    private String name;
}
