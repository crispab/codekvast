package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.controller.RegistrationController;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

/**
 * @author olle.hallin@crisp.se
 */
public interface RegistrationService {
    enum UniqueKind {USERNAME, EMAIL_ADDRESS, ORGANISATION_NAME}

    /**
     * Tests whether a name is unique.
     *
     * @param kind What kind of name?
     * @param name The name to test.
     * @return true iff that kind of name is unique.
     */
    boolean isUnique(UniqueKind kind, String name);

    /**
     * Creates a user and organisation. Assigns roles ADMIN and USER to the new USER. Inserts the new user as primary contact and ADMIN
     * for the
     * organisation.
     *
     * @param data The registration data.
     * @return The id of the created user.
     * @throws se.crisp.codekvast.server.codekvast_server.exception.CodekvastException If anything fails.
     */
    long registerUserAndOrganisation(RegistrationController.RegistrationRequest data) throws CodekvastException;

}
