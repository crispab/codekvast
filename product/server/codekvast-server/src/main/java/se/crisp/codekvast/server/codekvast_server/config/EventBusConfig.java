package se.crisp.codekvast.server.codekvast_server.config;

import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Guava event bus.
 *
 * @author Olle Hallin
 */
@Configuration
public class EventBusConfig {

    @Bean
    public EventBus eventBus() {
        return new EventBus();
        // return new AsyncEventBus(Executors.newSingleThreadExecutor());
    }
}
