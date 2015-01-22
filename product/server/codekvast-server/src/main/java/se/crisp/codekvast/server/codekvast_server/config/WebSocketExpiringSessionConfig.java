package se.crisp.codekvast.server.codekvast_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ExpiringSession;
import org.springframework.session.web.socket.events.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Trick to make Spring detect web socket session expiration.
 * <p/>
 * Without this, no {@link SessionDisconnectEvent}s will be published.
 *
 * @author Olle Hallin
 */
@Configuration
public class WebSocketExpiringSessionConfig<S extends ExpiringSession> {

    @Bean
    public WebSocketConnectHandler<S> webSocketConnectHandler() {
        return new WebSocketConnectHandler();
    }

    @Bean
    public WebSocketDisconnectHandler<S> webSocketDisconnectHandler() {
        return new WebSocketDisconnectHandler();
    }

    @Slf4j
    private static class WebSocketConnectHandler<S> implements ApplicationListener<SessionConnectEvent> {
        @Override
        public void onApplicationEvent(SessionConnectEvent event) {
            log.debug("On {}", event.getClass().getSimpleName());
        }
    }

    @Slf4j
    private static class WebSocketDisconnectHandler<S> implements ApplicationListener<SessionDisconnectEvent> {
        @Override
        public void onApplicationEvent(SessionDisconnectEvent event) {
            log.debug("On {}", event.getClass().getSimpleName());
        }
    }

}
