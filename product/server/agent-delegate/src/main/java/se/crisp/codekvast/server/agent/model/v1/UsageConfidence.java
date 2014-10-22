package se.crisp.codekvast.server.agent.model.v1;

/**
 * @author Olle Hallin
 */
public enum UsageConfidence {
    /**
     * The used signature was found as-is in the scanned code base.
     */
    EXACT_MATCH,

    /**
     * The used signature was <em>not</em> found as-is in the scanned code base. It was found however, when searching upwards in the class
     * hierarchy. The reason for not finding it in the first place could be that the method was synthesized at runtime by some byte code
     * manipulating AOP framework (like Spring or Guice).
     */
    FOUND_IN_PARENT_CLASS,

    /**
     * The used signature was <em>not</em> found at all in the scanned code base. This indicates a problem with the code base scanner.
     * Access to the source code is required in order to resolve the problem.
     */
    NOT_FOUND_IN_CODE_BASE;

    /**
     * Converts a UsageConfidence.ordinal() back to the enum constant.
     *
     * @param ordinal An integer returned by UsageConfidence.ordinal()
     * @return The proper enum constant
     * @throws java.lang.IllegalArgumentException If invalid ordinal value.
     */
    public static UsageConfidence fromOrdinal(int ordinal) {
        for (UsageConfidence confidence : UsageConfidence.values()) {
            if (confidence.ordinal() == ordinal) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unknown " + UsageConfidence.class.getSimpleName() + " ordinal: " + ordinal);
    }
}
