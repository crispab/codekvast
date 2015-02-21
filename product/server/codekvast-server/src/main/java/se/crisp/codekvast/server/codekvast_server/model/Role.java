package se.crisp.codekvast.server.codekvast_server.model;

/**
 * The different roles in the authorization system.
 *
 * @author olle.hallin@crisp.se
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
