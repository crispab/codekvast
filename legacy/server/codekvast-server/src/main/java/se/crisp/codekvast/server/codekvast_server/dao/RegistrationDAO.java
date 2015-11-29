package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.codekvast_server.model.Role;

/**
 * A data access object for things related to registration.
 *
 * @author olle.hallin@crisp.se
 */
public interface RegistrationDAO {
    /**
     * Count the number of users with this username.
     *
     * @param username
     * @return 0 or 1 (since usernames must be unique).
     */
    int countUsersByUsername(String username);

    /**
     * Count the number of users with this email address.
     *
     * @param emailAddress
     * @return 0 or 1 (since email addresses must be unique).
     */
    int countUsersByEmailAddress(String emailAddress);

    /**
     * Count the number of organisations with this name (lower cased).
     *
     * @param organisationName
     * @return 0 or 1. Since organisation names must be unique.
     */
    int countOrganisationsByNameLc(String organisationName);

    /**
     * Create a new user
     *
     * @param fullName
     * @param username
     * @param emailAddress
     * @param plaintextPassword
     * @param roles
     * @return The new user's primary key.
     * @throws DataAccessException
     */
    long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles)
            throws DataAccessException;

    /**
     * Create a new organisation.
     *
     * @param organisationName
     * @param userId
     * @throws DataAccessException
     */
    void createOrganisationWithPrimaryContact(String organisationName, long userId) throws DataAccessException;


}
