package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDataMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.CollectorSettings;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import javax.validation.Valid;
import java.security.Principal;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

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
    @NonNull
    private final Validator validator;


    @Inject
    public SignatureHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService, UserHandler userHandler,
                            Validator validator) {
        super(eventBus, messagingTemplate);
        this.userService = userService;
        this.userHandler = userHandler;
        this.validator = validator;
    }

    @InitBinder
    private void initBinder(WebDataBinder binder) {
        binder.setValidator(checkNotNull(validator, "validator is null"));
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    private void onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failure: " + e);
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private void onApplicationException(CodekvastException e) {
        log.error("Application exception: " + e);
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

    @RequestMapping(value = "/api/collectorSettings", method = RequestMethod.POST)
    public void saveCollectorSettings(@Valid @RequestBody CollectorSettings collectorSettings, Principal principal)
            throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' persists collector settings {}", username, collectorSettings);

        userService.saveCollectorSettings(username, collectorSettings);

        log.info("'{}' saved collector settings {}", username, collectorSettings);
    }

}
