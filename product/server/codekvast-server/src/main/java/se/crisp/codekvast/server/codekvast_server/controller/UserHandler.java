package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.crisp.codekvast.server.codekvast_server.model.event.internal.UserConnectedEvent;
import se.crisp.codekvast.server.codekvast_server.model.event.internal.UserDisconnectedEvent;

import javax.inject.Inject;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for maintaining a collection of currently connected web socket usernames.
 *
 * It translates Spring's SessionConnectedEvent and SessionDisconnected events to UserConnectedEvent and UserDisconnected events.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class UserHandler extends AbstractMessageHandler {

    private final Map<String, String> sessionIdToUsername = new HashMap<>();
    private final Set<String> presentUsers = new HashSet<>();

    private final Object lock = new Object();

    @Inject
    public UserHandler(EventBus eventBus) {
        super(eventBus);
    }

    /**
     * A web socket user has logged in.
     */
    @Subscribe
    @AllowConcurrentEvents
    public void onSessionConnected(SessionConnectedEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        MessageHeaders headers = event.getMessage().getHeaders();
        Principal user = SimpMessageHeaderAccessor.getUser(headers);
        if (user == null) {
            log.warn("Cannot find principal in {}, ignoring...", event);
            return;
        }

        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        if (sessionId == null) {
            log.warn("No sessionId in {}, ignoring...", event);
            return;
        }

        String username = user.getName();
        if (username == null) {
            log.warn("No name in {}, ignoring...", user);
            return;
        }

        synchronized (lock) {
            sessionIdToUsername.put(sessionId, username);
            presentUsers.add(username);
        }
        log.info("Added username '{}'", username);
        eventBus.post(new UserConnectedEvent(username));
    }

    /**
     * A web socket user leaves.
     */
    @Subscribe
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        String sessionId = event.getSessionId();
        if (sessionId == null) {
            log.warn("No sessionId in {}, ignoring...", event);
            return;
        }

        String username;
        synchronized (lock) {
            username = sessionIdToUsername.remove(sessionId);
            if (username == null) {
                log.warn("Cannot find username for session {}, ignoring...", sessionId);
                return;
            }
            presentUsers.remove(username);
        }

        if (username != null) {
            log.info("Removed username '{}'", username);
            eventBus.post(new UserDisconnectedEvent(username));
        }
    }

    public boolean isPresent(String username) {
        synchronized (lock) {
            return presentUsers.contains(username);
        }
    }

}
