package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;

import java.util.Collection;

/**
 * Responsible for user-facing services.
 *
 * @author olle.hallin@crisp.se
 */
public interface UserService {
    /**
     * Retrieve all signatures that a certain user has access to.
     *
     * @param username The logged in user's username
     * @return A list of signature display objects. Does never return null.
     */
    Collection<SignatureDisplay> getSignatures(String username) throws CodekvastException;

    /**
     * Retrieve collector status message for the organisation the user belongs to.
     * @param username The logged in user's username
     * @return The same type of event that is broadcast each time a collector has delivered new data.
     */
    CollectorStatusMessage getCollectorStatusMessage(String username) throws CodekvastException;
}
