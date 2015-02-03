package se.crisp.codekvast.server.codekvast_server.model;

/**
 * @author Olle Hallin
 */
public enum Role {
    SUPERUSER, AGENT, USER, ADMIN, MONITOR;

    public static final String ANNOTATION_PREFIX = "ROLE_";

    /**
     * The name to use in security-related annotations.
     */
    public String annotationName() {
        return ANNOTATION_PREFIX + name();
    }
}
