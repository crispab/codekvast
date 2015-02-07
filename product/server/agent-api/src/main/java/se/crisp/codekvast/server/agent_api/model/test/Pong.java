package se.crisp.codekvast.server.agent_api.model.test;

import lombok.*;
import lombok.experimental.Builder;

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
public class Pong {
    private String message;
}
