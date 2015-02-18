package se.crisp.codekvast.web.model;

import lombok.*;

/**
 * @author olle.hallin@crisp.se
 */
@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RegistrationResponse {
    private String greeting;
}
