package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Collection;

/**
 * A Spring MVC Controller that handles STOMP messages.
 * <p/>
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class StompController {
    public static final String TOPIC_SIGNATURES = "/topic/signatures";

    private final StorageService storageService;

    @Inject
    public StompController(StorageService storageService) {
        this.storageService = storageService;
    }

    @MessageMapping("/hello/**")
    @SendTo(TOPIC_SIGNATURES)
    public Collection<UsageDataEntry> hello(Message message, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("Received {} from '{}'", message, username);

        Collection<UsageDataEntry> signatures = storageService.getSignatures(null);

        log.debug("Returning {} signatures", signatures.size());
        return signatures;
    }
}
