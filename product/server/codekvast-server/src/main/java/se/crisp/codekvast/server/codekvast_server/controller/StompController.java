package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.security.Principal;
import java.util.Collection;

/**
 * A controller that handles STOMP messages from web socket clients.
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class StompController {
    public static final String TOPIC_SIGNATURES = "/topic/signatures";

    private final EventBus eventBus;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Inject
    public StompController(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
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
            messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, agentService.getSignatures(event.getOrganisationName()));
        } catch (CodekvastException e) {
            log.warn("Cannot get signatures", e);
        }
        */
        messagingTemplate.convertAndSend(StompController.TOPIC_SIGNATURES, event.getInvocationEntries());
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
