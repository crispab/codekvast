package se.crisp.codekvast.server.codekvast_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Configures the STOMP and WebSocket messaging.
 *
 * @author Olle Hallin
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class MessagingConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
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
