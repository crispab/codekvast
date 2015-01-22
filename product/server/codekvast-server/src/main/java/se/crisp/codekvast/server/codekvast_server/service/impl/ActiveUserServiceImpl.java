package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Repository;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;
import se.crisp.codekvast.server.codekvast_server.service.ActiveUserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class ActiveUserServiceImpl implements ActiveUserService {

    private final Map<String, ActiveUser> activeUsers = new ConcurrentHashMap<>();
    private final EventBus eventBus;

    @Inject
    public ActiveUserServiceImpl(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    void registerOnEventBus() {
        eventBus.register(this);
    }

    @PreDestroy
    void unregisterFromEventBus() {
        eventBus.unregister(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSessionConnected(SessionConnectedEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        MessageHeaders headers = event.getMessage().getHeaders();
        Principal user = SimpMessageHeaderAccessor.getUser(headers);
        if (user == null) {
            return;
        }
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        ActiveUser activeUser = ActiveUser.builder().sessionId(sessionId).username(user.getName()).loggedInAt(new Date()).build();
        activeUsers.put(activeUser.getSessionId(), activeUser);
        log.debug("Added {}", activeUser);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        String sessionId = event.getSessionId();
        ActiveUser removedUser = sessionId == null ? null : activeUsers.remove(sessionId);
        if (removedUser != null) {
            log.debug("Removed {} {}", sessionId, removedUser);
        }
    }

}
