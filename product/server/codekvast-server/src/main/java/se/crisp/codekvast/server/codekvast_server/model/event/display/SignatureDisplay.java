package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@EqualsAndHashCode(of = "name")
public class SignatureDisplay {
    String name;
    long invokedAtMillis;
    long millisSinceJvmStart;
}
