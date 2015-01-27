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
public class Organisation {
    private final long id;

    @NonNull
    @NotBlank
    private String name;
}
