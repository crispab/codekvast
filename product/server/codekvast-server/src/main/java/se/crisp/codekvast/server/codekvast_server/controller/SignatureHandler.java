package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
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
        SignatureDataMessage message = toSignatureDataMessage(null, event.getInvocationEntries());
        for (String username : event.getUsernames()) {
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
     * @return A SignatureDataMessage containing all signatures the principal has rights to view.
     */
    @RequestMapping("/api/signatures")
    public SignatureDataMessage getSignatures(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' requests all signatures", username);

        CollectorDataEvent collectorDataEvent = userService.getCollectorDataEvent(username);
        Collection<SignatureEntry> signatures = userService.getSignatures(username);
        return toSignatureDataMessage(collectorDataEvent.getCollectors(), signatures);
    }

    private SignatureDataMessage toSignatureDataMessage(Collection<CollectorEntry> collectors, Collection<SignatureEntry> signatures) {
        List<Signature> sig = new ArrayList<>();
        for (SignatureEntry entry : signatures) {
            sig.add(Signature.builder()
                             .name(entry.getSignature())
                             .invokedAtMillis(entry.getInvokedAtMillis())
                             .invokedAtString(DateUtils.formatDate(entry.getInvokedAtMillis()))
                             .millisSinceJvmStart(entry.getMillisSinceJvmStart())
                             .build());
        }
        return SignatureDataMessage.builder()
                                   .collectorStatus(collectors == null ? null : toCollectorStatusMessage(collectors))
                                   .signatures(sig)
                                   .build();
    }

    private CollectorStatusMessage toCollectorStatusMessage(Collection<CollectorEntry> collectors) {
        List<Collector> displayCollectors = new ArrayList<>();
        for (CollectorEntry entry : collectors) {
            displayCollectors.add(
                    Collector.builder()
                             .name(entry.getName())
                             .version(entry.getVersion())
                             .collectorStartedAtMillis(entry.getStartedAtMillis())
                             .collectorStartedAt(DateUtils.formatDate(entry.getStartedAtMillis()))
                             .trulyDeadAfterSeconds(entry.getTrulyDeadAfterSeconds())
                             .updateReceivedAtMillis(entry.getDumpedAtMillis())
                             .updateReceivedAt(DateUtils.formatDate(entry.getDumpedAtMillis()))
                             .build());
        }

        return CollectorStatusMessage.builder().collectors(displayCollectors).build();
    }

    // --- JSON objects -----------------------------------------------------------------------------------

    @Value
    @Builder
    static class CollectorStatusMessage {
        Collection<Collector> collectors;
    }

    @Value
    @Builder
    static class Collector {
        String name;
        String version;
        long collectorStartedAtMillis;
        int trulyDeadAfterSeconds;
        String collectorStartedAt;
        long updateReceivedAtMillis;
        String updateReceivedAt;
    }

    @Value
    @Builder
    static class Signature {
        String name;
        long invokedAtMillis;
        String invokedAtString;
        long millisSinceJvmStart;
    }

    @Value
    @Builder
    static class SignatureDataMessage {
        CollectorStatusMessage collectorStatus;
        List<Signature> signatures;
    }
}
