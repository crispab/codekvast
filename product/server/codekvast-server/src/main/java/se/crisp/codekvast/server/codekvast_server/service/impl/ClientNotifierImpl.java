package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.controller.StompController;
import se.crisp.codekvast.server.codekvast_server.event.internal.UsageDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;

/**
 * @author Olle Hallin
 */
@Slf4j
@Component
public class ClientNotifierImpl implements ApplicationListener<UsageDataUpdatedEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final StorageService storageService;

    @Inject
    public ClientNotifierImpl(SimpMessagingTemplate messagingTemplate, StorageService storageService) {
        this.messagingTemplate = messagingTemplate;
        this.storageService = storageService;
    }

    @Override
    public void onApplicationEvent(UsageDataUpdatedEvent event) {
        log.debug("Handling {}", event);
        // TODO: only send the new or updated signatures
        messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, storageService.getSignatures());
    }
}
