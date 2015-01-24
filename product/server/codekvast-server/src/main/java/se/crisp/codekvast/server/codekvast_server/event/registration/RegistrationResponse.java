package se.crisp.codekvast.server.codekvast_server.event.registration;

import lombok.*;
import lombok.experimental.Builder;

/**
 * Sent back to the JavaScript layer as successful response to a RegistrationRequest.
 *
 * @author Olle Hallin
 */
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RegistrationResponse {
    private String greeting;
}
