package se.crisp.codekvast.server.codekvast_server.config;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Configures the Guava event bus.
 *
 * It uses the property codekvast.eventBusThreads for configuring how many handler threads to create.
 * Setting this value to zero or negative value will create a synchronous event bus, i.e., the event handling is done in the sender's
 * thread.
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
public class EventBusConfig {

    @Bean
    public EventBus eventBus(CodekvastSettings settings) {
        int threads = settings.getEventBusThreads();
        return threads <= 0 ? new EventBus() : new AsyncEventBus(Executors.newFixedThreadPool(threads));
    }
}
