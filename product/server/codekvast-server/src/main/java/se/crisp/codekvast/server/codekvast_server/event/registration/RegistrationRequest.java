package se.crisp.codekvast.server.codekvast_server.event.registration;

import lombok.*;
import lombok.experimental.Builder;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import se.crisp.codekvast.server.agent.model.v1.Constraints;

import javax.validation.constraints.Size;

/**
 * Sent from JavaScript as the final step in the registration wizard.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RegistrationRequest {
    @NotBlank
    @Size(max = Constraints.MAX_FULL_NAME_LENGTH)
    private String fullName;

    @NotBlank
    @Size(max = Constraints.MAX_EMAIL_ADDRESS_LENGTH)
    @Email
    private String emailAddress;

    @NotBlank
    @Size(max = Constraints.MAX_USER_NAME_LENGTH)
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    @Size(max = Constraints.MAX_CUSTOMER_NAME_LENGTH)
    private String customerName;
}
