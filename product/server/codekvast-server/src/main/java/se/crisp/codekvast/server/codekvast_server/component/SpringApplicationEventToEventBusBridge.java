package se.crisp.codekvast.server.codekvast_server.component;

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
 * and you can have more than one handler method in the same class.
 *
 * @author Olle Hallin
 */
@Component
@Slf4j
public class SpringApplicationEventToEventBusBridge implements ApplicationListener {

    private final EventBus eventBus;

    @Inject
    public SpringApplicationEventToEventBusBridge(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        log.debug("On {}", event.getClass().getSimpleName());
        eventBus.post(event);
    }

}
