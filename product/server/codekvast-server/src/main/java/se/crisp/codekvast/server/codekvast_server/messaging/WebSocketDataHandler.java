package se.crisp.codekvast.server.codekvast_server.messaging;

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
import se.crisp.codekvast.server.codekvast_server.model.event.display.ApplicationStatisticsMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.CollectorStatusMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.display.InitialDataMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.CollectorSettings;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import javax.validation.Valid;
import java.security.Principal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for sending data messages to the correct users.
 *
 * @author olle.hallin@crisp.se
 */
@RestController
@Slf4j
public class WebSocketDataHandler extends AbstractEventBusSubscriber {
    @NonNull
    private final UserService userService;
    @NonNull
    private final WebSocketUserPresenceHandler webSocketUserPresenceHandler;
    @NonNull
    private final Validator validator;


    @Inject
    public WebSocketDataHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService,
                                WebSocketUserPresenceHandler webSocketUserPresenceHandler,
                                Validator validator) {
        super(eventBus, messagingTemplate);
        this.userService = userService;
        this.webSocketUserPresenceHandler = webSocketUserPresenceHandler;
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
            if (webSocketUserPresenceHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", message, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/collector/status", message);
            }
        }
    }

    /**
     * Send the message to the currently logged in users that are affected by the message.
     */
    @Subscribe
    public void onApplicationStatisticsMessage(ApplicationStatisticsMessage message) {
        for (String username : message.getUsernames()) {
            if (webSocketUserPresenceHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", message, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/application/statistics", message);
            }
        }
    }

    /**
     * A REST endpoint for getting initial data for the web interface..
     *
     * @param principal The identity of the authenticated user.
     * @return An InitialDataMessage containing all data needed to inflate the web interface.
     */
    @RequestMapping("/api/web/initialData")
    public InitialDataMessage getInitialData(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' requests initial data", username);

        ApplicationStatisticsMessage appStats = userService.getApplicationStatisticsMessage(username);
        CollectorStatusMessage collectorStatus = userService.getCollectorStatusMessage(username);

        return InitialDataMessage.builder()
                                   .applicationStatistics(appStats)
                                   .collectorStatus(collectorStatus)
                                   .build();
    }

    @RequestMapping(value = "/api/web/settings", method = RequestMethod.POST)
    public void saveSettings(@Valid @RequestBody CollectorSettings collectorSettings, Principal principal)
            throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' persists collector settings {}", username, collectorSettings);

        userService.saveCollectorSettings(username, collectorSettings);

        log.info("'{}' saved collector settings {}", username, collectorSettings);
    }

}
