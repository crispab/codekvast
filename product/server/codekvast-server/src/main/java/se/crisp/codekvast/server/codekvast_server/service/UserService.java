package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.RegistrationRequest;

/**
 * @author Olle Hallin
 */
public interface UserService {
    enum UniqueKind {USERNAME, CUSTOMER_NAME;}

    /**
     * Tests whether a name is unique.
     *
     * @param kind What kind of name?
     * @param name The name to test.
     * @return true iff that kind of name is unique.
     */
    boolean isUnique(UniqueKind kind, String name);

    /**
     * Creates a user and customer. Assigns roles ADMIN and USER to the new USER. Inserts the new user as primary contact and ADMIN for the
     * customer.
     *
     * @param data The registration data.
     * @return The id of the created user.
     * @throws CodekvastException If anything fails.
     */
    long registerUserAndCustomer(RegistrationRequest data) throws CodekvastException;
}
