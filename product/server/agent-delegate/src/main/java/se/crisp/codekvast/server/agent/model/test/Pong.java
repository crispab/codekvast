package se.crisp.codekvast.server.agent.model.test;

import lombok.*;
import lombok.experimental.Builder;

/**
 * Test class used in integration tests of the REST interface.
 *
 * @author Olle Hallin
 */
@Data
@Builder
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Pong {
    private String message;
}
