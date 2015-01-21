package se.crisp.codekvast.server.codekvast_server.websocket;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import se.crisp.codekvast.server.codekvast_server.dao.ActiveUserRepository;

/**
 * @author Olle Hallin
 */
public class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {
    private final ActiveUserRepository repository;
    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketDisconnectHandler(ActiveUserRepository repository, SimpMessageSendingOperations messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        repository.removeActiveUser(event.getSessionId());
    }
}

