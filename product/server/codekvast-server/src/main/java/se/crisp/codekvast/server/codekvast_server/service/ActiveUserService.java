package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;

import java.util.Collection;

/**
 * Service for retrieving active users.
 *
 * @author Olle Hallin
 */
public interface ActiveUserService {
    /**
     * Retrieve all active users.
     *
     * @return Does never return null
     */
    Collection<ActiveUser> getActiveUsers();
}
