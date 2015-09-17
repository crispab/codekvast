package se.crisp.codekvast.server.daemon_api.model.v1;

/**
 * @author olle.hallin@crisp.se
 */
public enum SignatureConfidence {
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
     * Converts a SignatureConfidence.ordinal() back to the enum constant.
     *
     * @param ordinal An Integer returned by SignatureConfidence.ordinal(). May be null.
     * @return The proper enum constant or null if {@code ordinal} is null.
     * @throws java.lang.IllegalArgumentException If invalid ordinal value other than null.
     */
    public static SignatureConfidence fromOrdinal(Integer ordinal) {
        if (ordinal == null) {
            return null;
        }
        for (SignatureConfidence confidence : SignatureConfidence.values()) {
            if (confidence.ordinal() == ordinal) {
                return confidence;
            }
        }
        throw new IllegalArgumentException("Unknown " + SignatureConfidence.class.getSimpleName() + " ordinal: " + ordinal);
    }
}
