package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.codekvast_server.dao.ActiveUserRepository;
import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class ActiveUserRepositoryImpl implements ActiveUserRepository {

    private final Map<String, ActiveUser> activeUsers = new ConcurrentHashMap<>();

    @Override
    public void addActiveUser(ActiveUser activeUser) {
        log.debug("Adding {}", activeUser);
        activeUsers.put(activeUser.getSessionId(), activeUser);
    }

    @Override
    public ActiveUser removeActiveUser(String sessionId) {
        ActiveUser activeUser = sessionId == null ? null : activeUsers.remove(sessionId);
        if (activeUser == null) {
            log.warn("Cannot find active user with sessionID {}", sessionId);
        } else {
            log.debug("Removed {} {}", sessionId, activeUser);
        }
        return activeUser;
    }
}
