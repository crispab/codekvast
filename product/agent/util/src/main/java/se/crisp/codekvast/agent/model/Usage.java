package se.crisp.codekvast.agent.model;

import lombok.Value;

/**
 * Holds data about the usage of one method signature.
 *
 * @author Olle Hallin
 */
@Value
public class Usage {
    private final String signature;
    private final long usedAtMillis;
}
