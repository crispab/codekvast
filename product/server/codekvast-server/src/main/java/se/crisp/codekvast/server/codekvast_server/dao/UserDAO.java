package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException;
import se.crisp.codekvast.server.codekvast_server.model.Role;

import java.util.Collection;

/**
 * A data access object for things related to interactive users.
 *
 * @author olle.hallin@crisp.se
 */
public interface UserDAO {

    /**
     * Translates a username to a organisation ID
     *
     * @param username A real user's login name or an agent's agentAccessID
     * @return The organisation ID for that user or agent.
     * @throws se.crisp.codekvast.server.codekvast_server.exception.UndefinedUserException
     */
    long getOrganisationIdForUsername(String username) throws UndefinedUserException;

    /**
     * Which interactive usernames does an organisation contain?
     *
     * @param organisationId
     * @return All usernames in the organisation which have the role {@link Role#USER}.
     */
    Collection<String> getInteractiveUsernamesInOrganisation(long organisationId);
}
