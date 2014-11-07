package se.crisp.codekvast.server.codekvast_server.model;

import lombok.*;
import lombok.experimental.Builder;

/**
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
