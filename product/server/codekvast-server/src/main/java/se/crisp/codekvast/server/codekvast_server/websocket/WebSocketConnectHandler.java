package se.crisp.codekvast.server.codekvast_server.websocket;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import se.crisp.codekvast.server.codekvast_server.dao.ActiveUserRepository;
import se.crisp.codekvast.server.codekvast_server.model.ActiveUser;

import java.security.Principal;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Olle Hallin
 */
public class WebSocketConnectHandler<S> implements ApplicationListener<SessionConnectEvent> {
    private final ActiveUserRepository repository;
    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketConnectHandler(ActiveUserRepository repository, SimpMessageSendingOperations messagingTemplate) {
        super();
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public void onApplicationEvent(SessionConnectEvent event) {
        MessageHeaders headers = event.getMessage().getHeaders();
        Principal user = SimpMessageHeaderAccessor.getUser(headers);
        if (user == null) {
            return;
        }
        String id = SimpMessageHeaderAccessor.getSessionId(headers);
        repository.addActiveUser(ActiveUser.builder().sessionId(id).username(user.getName()).loggedInAt(new Date()).build());
        messagingTemplate.convertAndSend("/topic/friends/signin", Arrays.asList(user.getName()));
    }
}
