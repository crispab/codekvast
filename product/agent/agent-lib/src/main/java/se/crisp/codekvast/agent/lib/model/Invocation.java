package se.crisp.codekvast.agent.lib.model;

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
