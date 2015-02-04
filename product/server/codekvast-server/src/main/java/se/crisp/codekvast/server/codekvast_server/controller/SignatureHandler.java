package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
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
 * @author Olle Hallin
 */
@Controller
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
        long now = System.currentTimeMillis();
        Timestamp timestamp = toStompTimestamp(now, event.getCollectors());

        for (String username : event.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", timestamp, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/timestamps", timestamp);
            }
        }
    }

    @Subscribe
    public void onInvocationDataUpdatedEvent(InvocationDataUpdatedEvent event) throws CodekvastException {
        SignatureData data = toSignatureData(event.getInvocationEntries());
        for (String username : event.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} signatures to '{}'", data.getSignatures().size(), username);
                messagingTemplate.convertAndSendToUser(username, "/queue/signatureUpdates", data);
            }
        }
    }

    /**
     * The JavaScript layer requests all signatures.
     *
     * @param principal The identity of the authenticated user.
     * @return All signatures that this user has permission to view
     */
    @SubscribeMapping("/signatures")
    public SignatureData subscribeSignatures(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' is subscribing to signatures", username);

        // Make sure the user gets the collector data event immediately...
        CollectorDataEvent event = userService.getCollectorDataEvent(username);
        onCollectorDataEvent(event);

        SignatureData data = toSignatureData(userService.getSignatures(username));
        log.debug("Sending {} signatures to '{}'", data.getSignatures().size(), username);
        return data;
    }

    private SignatureData toSignatureData(Collection<SignatureEntry> invocationEntries) {
        long now = System.currentTimeMillis();

        List<Signature> signatures = new ArrayList<>();

        for (SignatureEntry entry : invocationEntries) {
            String s = entry.getSignature();

            long invokedAtMillis = entry.getInvokedAtMillis();

            signatures.add(Signature.builder()
                                    .name(s)
                                    .invokedAtMillis(invokedAtMillis)
                                    .invokedAtString(DateUtils.formatDate(invokedAtMillis))
                                    .build());
        }

        return SignatureData.builder()
                            .signatures(signatures)
                            .build();
    }

    private Timestamp toStompTimestamp(long now, Collection<CollectorEntry> collectors) {
        long startedAt = Long.MAX_VALUE;
        long updatedAt = Long.MIN_VALUE;
        List<String> collectorStrings = new ArrayList<>();

        for (CollectorEntry entry : collectors) {
            startedAt = Math.min(startedAt, entry.getStartedAtMillis());
            updatedAt = Math.max(updatedAt, entry.getDumpedAtMillis());
            collectorStrings.add(String.format("%s started %s (%s), updated %s",
                                               entry.getName(),
                                               DateUtils.formatDate(entry.getStartedAtMillis()),
                                               DateUtils.getAge(now, entry.getStartedAtMillis()),
                                               DateUtils.formatDate(entry.getDumpedAtMillis())));
        }

        return Timestamp.builder()
                        .collectionStartedAt(DateUtils.formatDate(startedAt))
                        .collectionAge(DateUtils.getAge(now, startedAt))
                        .updateReceivedAt(DateUtils.formatDate(updatedAt))
                        .updateAge(DateUtils.getAge(now, updatedAt))
                        .collectors(collectorStrings)
                        .build();
    }

    @Value
    @Builder
    static class SignatureData {
        @NonNull
        private final List<Signature> signatures;
    }

    @Value
    @Builder
    static class Timestamp {
        @NonNull
        private final String collectionStartedAt;
        @NonNull
        private final String collectionAge;
        @NonNull
        private final String updateReceivedAt;
        @NonNull
        private final String updateAge;
        @NonNull
        private final Collection<String> collectors;
    }

    @Value
    @Builder
    static class Signature {
        @NonNull
        private final String name;
        private final long invokedAtMillis;
        @NonNull
        private final String invokedAtString;
    }

}
