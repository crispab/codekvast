package se.crisp.codekvast.server.codekvast_server.event.registration;

import lombok.*;
import lombok.experimental.Builder;

/**
 * Sent in response to a IsNameUniqueRequest back to the JavaScript layer in the registration wizard.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class IsNameUniqueResponse {
    private boolean isUnique;
}
