package se.crisp.codekvast.server.agent_api.model.test;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Size;

/**
 * Test class used in integration tests of the REST interface.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Ping {
    @NonNull
    @NotBlank
    @Size(max = 10)
    private String message;
}
