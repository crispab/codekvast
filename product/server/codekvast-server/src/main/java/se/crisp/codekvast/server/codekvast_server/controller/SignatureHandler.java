package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorDataEvent.CollectorEntry;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;
import se.crisp.codekvast.server.codekvast_server.util.DateUtils;

import javax.inject.Inject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Responsible for sending signatures to the correct users.
 *
 * @author olle.hallin@crisp.se
 */
@Controller
@Slf4j
public class SignatureHandler extends AbstractMessageHandler {
    private final UserService userService;
    private final UserHandler userHandler;
    private final WebSocketSessionState sessionState;

    @Inject
    public SignatureHandler(EventBus eventBus, SimpMessagingTemplate messagingTemplate, UserService userService, UserHandler userHandler,
                            WebSocketSessionState sessionState) {
        super(eventBus, messagingTemplate);
        this.userService = userService;
        this.userHandler = userHandler;
        this.sessionState = sessionState;
    }

    @Subscribe
    public void onCollectorDataEvent(CollectorDataEvent event) {
        CollectorStatusMessage message = toCollectorStatusMessage(event.getCollectors());

        for (String username : event.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", message, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/collector/status", message);
            }
        }
    }

    @Subscribe
    public void onInvocationDataUpdatedEvent(InvocationDataUpdatedEvent event) throws CodekvastException {
        // TODO: stuff event data away and notify clients that there is signature data available
    }

    /**
     * A web socket client announces it's presence.
     *
     * @param principal The identity of the authenticated user.
     * @return A SignaturesAvailableMessage that kicks off pulling all signatures for that user.
     */
    @MessageMapping("/topic/hello")
    @SendToUser("/user/queue/signature/available")
    public SignaturesAvailableMessage subscribeSignatures(String greeting, Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' says {}", username, greeting);

        sessionState.setSignatures(userService.getSignatures(username));
        return sessionState.getSignaturesAvailableMessage();
    }

    @MessageMapping("/user/queue/signature/next")
    public SignatureDataMessage getNextSignatures(Principal principal) {
        log.debug("'{}' requests signatures", principal.getName());

        return sessionState.getNextSignatureDataMessage();
    }

    private CollectorStatusMessage toCollectorStatusMessage(Collection<CollectorEntry> collectors) {
        long startedAt = Long.MAX_VALUE;
        long updatedAt = Long.MIN_VALUE;
        List<Collector> displayCollectors = new ArrayList<>();
        boolean isEmpty = true;
        for (CollectorEntry entry : collectors) {
            startedAt = Math.min(startedAt, entry.getStartedAtMillis());
            updatedAt = Math.max(updatedAt, entry.getDumpedAtMillis());
            displayCollectors.add(
                    Collector.builder()
                             .name(entry.getName())
                             .version(entry.getVersion())
                             .collectorStartedAtMillis(entry.getStartedAtMillis())
                             .collectorStartedAt(DateUtils.formatDate(entry.getStartedAtMillis()))
                             .updateReceivedAtMillis(entry.getDumpedAtMillis())
                             .updateReceivedAt(DateUtils.formatDate(entry.getDumpedAtMillis()))
                             .build());
            isEmpty = false;
        }

        CollectorStatusMessage.CollectorStatusMessageBuilder builder = CollectorStatusMessage.builder().collectors(displayCollectors);
        if (isEmpty) {
            builder.collectionStartedAt("Waiting for collectors to start");
        } else {
            builder.collectionStartedAtMillis(startedAt)
                   .collectionStartedAt(DateUtils.formatDate(startedAt))
                   .updateReceivedAtMillis(updatedAt)
                   .updateReceivedAt(DateUtils.formatDate(updatedAt));
        }
        return builder.build();
    }

    // --- JSON objects -----------------------------------------------------------------------------------

    @Value
    @Builder
    static class SignaturesAvailableMessage {
        int pendingSignatures;
        Progress progress;
    }

    @Value
    @Builder
    static class Progress {
        String message;
        int value;
        int max;
    }

    @Value
    @Builder
    static class Signature {
        String name;
        long invokedAtMillis;
        String invokedAtString;
    }

    @Value
    @Builder
    static class SignatureDataMessage {
        boolean more;
        Progress progress;
        @Singular
        List<Signature> signatures;
    }

    @Value
    @Builder
    static class CollectorStatusMessage {
        long collectionStartedAtMillis;
        String collectionStartedAt;
        long updateReceivedAtMillis;
        String updateReceivedAt;
        Collection<Collector> collectors;
    }

    @Value
    @Builder
    static class Collector {
        String name;
        String version;
        long collectorStartedAtMillis;
        String collectorStartedAt;
        long updateReceivedAtMillis;
        String updateReceivedAt;
    }

}
