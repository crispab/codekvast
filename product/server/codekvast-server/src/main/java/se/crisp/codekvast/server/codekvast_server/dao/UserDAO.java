package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.Role;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;

import java.util.Collection;
import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
public interface UserDAO {

    /**
     * Translates a username to a organisation ID
     *
     * @param username A real user's login name or an agent's agentAccessID
     * @return The username for that user or agent.
     * @throws se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException
     */
    long getOrganisationIdForUsername(String username) throws UndefinedUserException;

    Collection<String> getInteractiveUsernamesInOrganisation(long organisationId);

    int countUsersByUsername(String username);

    int countUsersByEmailAddress(String emailAddress);

    int countOrganisationsByNameLc(String organisationName);

    long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles)
            throws DataAccessException;

    void createOrganisationWithPrimaryContact(String organisationName, long userId) throws DataAccessException;

    /**
     * Retrieve all signatures for a certain organisation
     */
    Set<SignatureDisplay> getSignatures(long organisationId);
}
