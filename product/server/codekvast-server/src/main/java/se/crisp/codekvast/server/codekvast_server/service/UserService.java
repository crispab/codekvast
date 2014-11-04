package se.crisp.codekvast.server.codekvast_server.service;

/**
 * @author Olle Hallin
 */
public interface UserService {
    enum UniqueKind {USERNAME, CUSTOMER_NAME}

    boolean isUnique(UniqueKind kind, String value);
}
