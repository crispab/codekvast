package se.crisp.codekvast.server.codekvast_server.messagehandler;

import com.google.common.eventbus.EventBus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Olle Hallin
 */
abstract class AbstractMessageHandler {
    protected final EventBus eventBus;
    protected final SimpMessagingTemplate messagingTemplate;

    protected AbstractMessageHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
    }

    protected AbstractMessageHandler(EventBus eventBus) {
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
