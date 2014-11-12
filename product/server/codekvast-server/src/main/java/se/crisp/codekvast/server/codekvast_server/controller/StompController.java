package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

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

    private final UserService userService;

    @Inject
    public StompController(UserService userService) {
        this.userService = userService;
    }

    @MessageMapping("/hello/**")
    @SendTo(TOPIC_SIGNATURES)
    public Collection<InvocationEntry> hello(Message message, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("Received {} from '{}'", message, username);

        Collection<InvocationEntry> signatures = userService.getSignatures(null);

        log.debug("Returning {} signatures", signatures.size());
        return signatures;
    }
}
