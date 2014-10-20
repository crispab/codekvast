package se.crisp.codekvast.server.agent.model.test;

import lombok.*;
import lombok.experimental.Builder;

import javax.validation.constraints.Size;

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
public class Ping {
    @NonNull
    @Size(min = 1, max = 10)
    private String message;
}
