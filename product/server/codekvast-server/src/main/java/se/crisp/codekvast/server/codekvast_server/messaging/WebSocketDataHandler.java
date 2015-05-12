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
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageRequest;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.GetMethodUsageResponse;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;
import se.crisp.codekvast.server.codekvast_server.service.ReportService;
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
    private final ReportService reportService;
    @NonNull
    private final WebSocketUserPresenceHandler webSocketUserPresenceHandler;
    @NonNull
    private final Validator validator;

    @Inject
    public WebSocketDataHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService,
                                ReportService reportService, WebSocketUserPresenceHandler webSocketUserPresenceHandler,
                                Validator validator) {
        super(eventBus, messagingTemplate);
        this.userService = userService;
        this.reportService = reportService;
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
    public void onWebSocketMessage(WebSocketMessage message) {
        message.getUsernames().stream().filter(username -> webSocketUserPresenceHandler.isPresent(username)).forEach(username -> {
            log.debug("Sending {} to '{}'", message, username);
            messagingTemplate.convertAndSendToUser(username, "/queue/data", message);
        });
    }

    /**
     * A REST endpoint for getting initial data for the web interface..
     *
     * @param principal The identity of the authenticated user.
     * @return An WebSocketMessage containing all data needed to inflate the web interface.
     */
    @RequestMapping("/api/web/data")
    public WebSocketMessage getInitialData(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' requests initial data", username);

        return userService.getWebSocketMessage(username);
    }

    @RequestMapping(value = "/api/web/settings", method = RequestMethod.POST)
    public void saveSettings(@Valid @RequestBody OrganisationSettings settings, Principal principal)
            throws CodekvastException {

        String username = principal.getName();
        log.debug("'{}' persists settings {}", username, settings);

        userService.saveOrganisationSettings(username, settings);

        log.info("'{}' saved settings {}", username, settings);
    }

    @RequestMapping(value = "/api/web/getMethodUsage", method = RequestMethod.POST)
    public GetMethodUsageResponse getMethodUsage(@Valid @RequestBody GetMethodUsageRequest request, Principal principal)
            throws CodekvastException {
        long startedAtMillis = System.currentTimeMillis();

        String username = principal.getName();
        log.debug("'{}' requests {}", username, request);

        GetMethodUsageResponse response = reportService.getMethodUsage(username, request);

        log.debug("Method usage response created in {} ms", System.currentTimeMillis() - startedAtMillis);

        return response;
    }
}
