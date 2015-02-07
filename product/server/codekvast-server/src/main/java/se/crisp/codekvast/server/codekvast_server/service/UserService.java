package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
public interface UserService {
    /**
     * Retrieve all signatures that a certain user has access to.
     *
     * @param username The logged in user's username
     * @return A list of invocation entries. Does never return null.
     */
    Collection<SignatureEntry> getSignatures(String username) throws CodekvastException;

    /**
     * Retrieve collector data for the organisation the user belongs to.
     * @param username The logged in user's username
     * @return The same type of event that is broadcasted each time a collector has delivered new data.
     */
    CollectorDataEvent getCollectorDataEvent(String username) throws CodekvastException;
}
