package se.crisp.codekvast.server.codekvast_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.ExpiringSession;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Configures the STOMP and WebSocket eventhandler.
 *
 * @author Olle Hallin
 */
@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig extends AbstractSessionWebSocketMessageBrokerConfigurer<ExpiringSession> {

    @Override
    protected void configureStompEndpoints(StompEndpointRegistry registry) {
        log.debug("Registering STOMP endpoints");
        registry.addEndpoint("/codekvast").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.debug("Configuring message broker");
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue");
    }

}
