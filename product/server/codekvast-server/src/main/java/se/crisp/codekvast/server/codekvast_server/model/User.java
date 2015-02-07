package se.crisp.codekvast.server.codekvast_server.model;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class User {
    @NonNull
    @NotBlank
    private final String fullName;

    @NonNull
    @Email
    private final String emailAddress;

    @NonNull
    @NotBlank
    private final String username;
}
