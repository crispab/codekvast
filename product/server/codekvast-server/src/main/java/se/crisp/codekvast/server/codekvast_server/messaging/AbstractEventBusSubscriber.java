package se.crisp.codekvast.server.codekvast_server.messaging;

import com.google.common.eventbus.EventBus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Base class for message-driven handlers
 *
 * @author olle.hallin@crisp.se
 */
public abstract class AbstractEventBusSubscriber {
    protected final EventBus eventBus;
    protected final SimpMessagingTemplate messagingTemplate;

    protected AbstractEventBusSubscriber(EventBus eventBus, SimpMessagingTemplate messagingTemplate) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
    }

    protected AbstractEventBusSubscriber(EventBus eventBus) {
        this(eventBus, null);
    }

    @PostConstruct
    void registerOnEventBus() {
        eventBus.register(this);
    }

    @PreDestroy
    void unregisterFromEventBus() {
        eventBus.unregister(this);
    }
}
