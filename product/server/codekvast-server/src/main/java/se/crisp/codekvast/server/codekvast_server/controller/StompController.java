package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.Application;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Collection;

/**
 * A Spring MVC Controller that handles STOMP messages from web clients.
 *
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class StompController {
    public static final String TOPIC_SIGNATURES = "/topic/signatures";

    private final UserService userService;

    @Inject
    public StompController(UserService userService) {
        this.userService = userService;
    }

    @SubscribeMapping("/applications")
    public Collection<Application> subscribeApplications(Message message, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' is subscribing for applications", message, username);

        Collection<Application> applications = userService.getApplications(username);

        log.debug("Returning {}", applications);
        return applications;
    }

    @SubscribeMapping("/signatures")
    public Collection<InvocationEntry> subscribeSignatures(Message message, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("Received {} from '{}'", message, username);

        Collection<InvocationEntry> signatures = userService.getSignatures(null);

        log.debug("Returning {} signatures", signatures.size());
        return signatures;
    }
}
