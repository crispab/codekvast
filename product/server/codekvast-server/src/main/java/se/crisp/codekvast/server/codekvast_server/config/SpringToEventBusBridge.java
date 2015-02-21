package se.crisp.codekvast.server.codekvast_server.config;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * A bridge from Spring ApplicationEvents to Guava EventBus.
 *
 * The EventBus is much easier to use, since you can name the event handling methods anything you like,
 * and you can have more than one handler method in the same class even for the same event type.
 *
 * It also handles asynchronous event delivery, which is critical when a {@literal @Transactional} method posts a message.
 * The receiver will handle the message in a different thread, and the transaction can commit immediately.
 *
 * In contrast, a Spring ApplicationListener handling method must be named onApplicationEvent() and the event processing
 * is done in the sender's thread.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class SpringToEventBusBridge implements ApplicationListener {

    private final EventBus eventBus;

    @Inject
    public SpringToEventBusBridge(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        log.trace("On {}", event.getClass().getSimpleName());
        eventBus.post(event);
    }

}
