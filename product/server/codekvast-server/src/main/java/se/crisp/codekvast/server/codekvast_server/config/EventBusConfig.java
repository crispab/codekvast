package se.crisp.codekvast.server.codekvast_server.config;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Configures the Guava event bus.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
public class EventBusConfig {

    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(Executors.newFixedThreadPool(10));
    }
}
