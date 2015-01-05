package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.controller.StompController;
import se.crisp.codekvast.server.codekvast_server.event.internal.ApplicationCreatedEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.CustomerCreatedEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.AgentService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ClientNotifierImpl {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messagingTemplate;
    private final AgentService agentService;

    @Inject
    public ClientNotifierImpl(EventBus eventBus, SimpMessagingTemplate messagingTemplate, AgentService agentService) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
        this.agentService = agentService;
    }

    @PostConstruct
    public void postConstruct() {
        eventBus.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        eventBus.unregister(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onCustomerCreated(CustomerCreatedEvent event) {
        log.debug("Handling {}", event);
        // TODO: notify STOMP subscribers
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        log.debug("Handling {}", event);
        // TODO: notify STOMP subscribers
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onInvocationDataUpdated(InvocationDataUpdatedEvent event) {
        if (log.isTraceEnabled()) {
            log.debug("Handling {}", event.toLongString());
        } else {
            log.debug("Handling {}", event);
        }

        // TODO: only send the new or updated signatures. This requires a change in codekvast.js as well, as it now expects a complete
        // collection of signatures.

        /*
        try {
            messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, agentService.getSignatures(event.getCustomerName()));
        } catch (CodekvastException e) {
            log.warn("Cannot get signatures", e);
        }
        */
        messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, event.getInvocationEntries());
    }
}
