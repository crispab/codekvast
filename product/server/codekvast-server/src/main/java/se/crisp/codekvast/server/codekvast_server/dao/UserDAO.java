package se.crisp.codekvast.server.codekvast_server.dao;

import org.springframework.dao.DataAccessException;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import java.util.Collection;

/**
 * @author Olle Hallin
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

    /**
     * Retrieve an application ID. If not found, a new row is inserted into APPLICATIONS and an ApplicationCreatedEvent is posted on the
     * event bus.
     */
    long getAppId(long organisationId, String appName, String appVersion) throws UndefinedApplicationException;

    AppId getAppIdByJvmFingerprint(String jvmFingerprint);

    int countUsersByUsername(String username);

    int countUsersByEmailAddress(String emailAddress);

    int countOrganisationsByNameLc(String organisationName);

    long createUser(String fullName, String username, String emailAddress, String plaintextPassword, Role... roles)
            throws DataAccessException;

    void createOrganisationWithPrimaryContact(String organisationName, long userId) throws DataAccessException;

    /**
     * Retrieve all signatures for a certain organisation
     */
    Collection<InvocationEntry> getSignatures(long organisationId);

    /**
     * Retrieve all applications for a certain organisation
     */
    Collection<Application> getApplications(long organisationId);

    /**
     * Return all usernames that belong to this organisation
     *
     * @param organisationId
     * @return Never null, an empty collection should organisationId be invalid.
     */
    Collection<String> getUsernamesInOrganisation(long organisationId);
}
