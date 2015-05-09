package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;

/**
 * Responsible for user-facing services.
 *
 * @author olle.hallin@crisp.se
 */
public interface UserService {
    /**
     * Retrieve collector status message for the organisation the user belongs to.
     * @param username The logged in user's username
     * @return The same type of event that is broadcast each time a collector has delivered new data.
     */
    WebSocketMessage getWebSocketMessage(String username) throws CodekvastException;

    /**
     * Saves collector settings.
     *
     * @param username          The username who made the request.
     * @param organisationSettings The new settings.
     */
    void saveOrganisationSettings(String username, OrganisationSettings organisationSettings) throws CodekvastException;

}
