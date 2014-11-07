package se.crisp.codekvast.server.codekvast_server.model;

import lombok.*;
import se.crisp.codekvast.server.agent.model.v1.Constraints;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegistrationRequest {
    @NotNull
    @Size(min = 1, max = Constraints.MAX_FULL_NAME_LENGTH)
    private String fullName;

    @NotNull
    @Size(min = 1, max = Constraints.MAX_EMAIL_ADDRESS_LENGTH)
    private String emailAddress;

    @NotNull
    @Size(min = 1, max = Constraints.MAX_USER_NAME_LENGTH)
    private String username;

    @NotNull
    @Size(min = 1)
    private String password;

    @NotNull
    @Size(min = 1, max = Constraints.MAX_CUSTOMER_NAME_LENGTH)
    private String customerName;

    @NotNull
    @Size(min = 1, max = Constraints.MAX_APP_NAME_LENGTH)
    private String appName;
}
