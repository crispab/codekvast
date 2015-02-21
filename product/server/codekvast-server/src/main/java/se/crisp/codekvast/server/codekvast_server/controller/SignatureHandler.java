package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDataMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Collection;

/**
 * Responsible for sending collector status and signature data messages to the correct users.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class SignatureHandler extends AbstractMessageHandler {
    private final UserService userService;
    private final UserHandler userHandler;

    @Inject
    public SignatureHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService, UserHandler userHandler) {
        super(eventBus, messagingTemplate);
        this.userService = userService;
        this.userHandler = userHandler;
    }

    /**
     * Send the message to the currently logged in users that are affected by the message.
     */
    @Subscribe
    public void onCollectorStatusMessage(CollectorStatusMessage message) {
        for (String username : message.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", message, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/collector/status", message);
            }
        }
    }

    /**
     * Send the message to the currently logged in users that are affected by the message.
     */
    @Subscribe
    public void onSignatureDataMessage(SignatureDataMessage message) throws CodekvastException {
        for (String username : message.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", message, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/signature/data", message);
            }
        }
    }

    /**
     * A REST endpoint for doing the initial get of signatures.
     *
     * @param principal The identity of the authenticated user.
     * @return A SignatureDataMessage containing all signatures the user has rights to view as well as an initial
     * CollectorStatusMessage
     */
    @RequestMapping("/api/signatures")
    public SignatureDataMessage getSignatureData(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' requests all signatures", username);

        CollectorStatusMessage collectorStatus = userService.getCollectorStatusMessage(username);
        Collection<SignatureDisplay> signatures = userService.getSignatures(username);

        return SignatureDataMessage.builder().collectorStatus(collectorStatus).signatures(signatures).build();
    }

}
