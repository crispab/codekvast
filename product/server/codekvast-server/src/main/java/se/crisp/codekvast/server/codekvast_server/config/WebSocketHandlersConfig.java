package se.crisp.codekvast.server.codekvast_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.session.ExpiringSession;
import se.crisp.codekvast.server.codekvast_server.dao.ActiveUserRepository;
import se.crisp.codekvast.server.codekvast_server.websocket.WebSocketConnectHandler;
import se.crisp.codekvast.server.codekvast_server.websocket.WebSocketDisconnectHandler;

/**
 * @author Olle Hallin
 */
@Configuration
public class WebSocketHandlersConfig<S extends ExpiringSession> {

    @Bean
    public WebSocketConnectHandler<S> webSocketConnectHandler(ActiveUserRepository repository,
                                                              SimpMessageSendingOperations messagingTemplate) {
        return new WebSocketConnectHandler<S>(repository, messagingTemplate);
    }

    @Bean
    public WebSocketDisconnectHandler webSocketDisconnectHandler(ActiveUserRepository repository,
                                                                 SimpMessageSendingOperations messagingTemplate) {
        return new WebSocketDisconnectHandler(repository, messagingTemplate);
    }
}
