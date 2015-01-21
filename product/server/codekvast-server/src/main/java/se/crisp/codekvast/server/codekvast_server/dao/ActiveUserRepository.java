package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;

/**
 * @author Olle Hallin
 */
public interface ActiveUserRepository {

    void addActiveUser(ActiveUser activeUser);

    ActiveUser removeActiveUser(String sessionId);

}
