package se.crisp.codekvast.shared.model;

import lombok.Value;

/**
 * Holds data about the invocation of one method.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class Invocation {
    private final String signature;
    private final long invokedAtMillis;
}
