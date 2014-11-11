package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.controller.StompController;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ClientNotifierImpl implements ApplicationListener<InvocationDataUpdatedEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final StorageService storageService;

    @Inject
    public ClientNotifierImpl(SimpMessagingTemplate messagingTemplate, StorageService storageService) {
        this.messagingTemplate = messagingTemplate;
        this.storageService = storageService;
    }

    @Override
    public void onApplicationEvent(InvocationDataUpdatedEvent event) {
        if (log.isTraceEnabled()) {
            log.debug("Handling {}", event.toLongString());
        } else {
            log.debug("Handling {}", event);
        }

        // TODO: only send the new or updated signatures. This requires a change in codekvast.js as well, as it now expects a complete
        // collection of signatures.

        /*
        try {
            messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, storageService.getSignatures(event.getCustomerName()));
        } catch (CodekvastException e) {
            log.warn("Cannot get signatures", e);
        }
        */
        messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, event.getInvocationEntries());
    }
}
