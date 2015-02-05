package se.crisp.codekvast.server.codekvast_server.controller;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Responsible for sending signatures to the correct users.
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class SignatureHandler extends AbstractMessageHandler {
    private static final int CHUNK_SIZE = 100;
    private final UserService userService;
    private final UserHandler userHandler;
    private final Executor executor = Executors.newFixedThreadPool(5);

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
                messagingTemplate.convertAndSendToUser(username, "/queue/collectorStatus", message);
            }
        }
    }

    @Subscribe
    public void onInvocationDataUpdatedEvent(InvocationDataUpdatedEvent event) throws CodekvastException {
        ChunkedSignatureSender sender = createChunkedSignatureSender(false, event.getUsernames(), null, event.getInvocationEntries());
        executor.execute(sender);
    }

    /**
     * The JavaScript layer requests all signatures.
     *
     * @param principal The identity of the authenticated user.
     * @return A SignatureMessage that bootstraps the angular view
     */
    @SubscribeMapping("/signatures")
    public SignatureMessage subscribeSignatures(Principal principal) throws CodekvastException {
        String username = principal.getName();
        log.debug("'{}' is subscribing to signatures", username);

        CollectorStatusMessage collectorStatusMessage = toCollectorStatusMessage(
                userService.getCollectorDataEvent(username)
                           .getCollectors());

        Collection<SignatureEntry> signatures = userService.getSignatures(username);

        ChunkedSignatureSender sender = createChunkedSignatureSender(true,
                                                                     Arrays.asList(username),
                                                                     collectorStatusMessage,
                                                                     signatures);
        SignatureMessage firstChunk = sender.nextChunk();

        try {
            log.debug("Sending first {} of {} signatures to '{}'", firstChunk.getSignatures().size(), signatures.size(), username);
            return firstChunk;
        } finally {
            executor.execute(sender);
        }
    }

    private ChunkedSignatureSender createChunkedSignatureSender(boolean sendProgress,
                                                                Collection<String> usernames,
                                                                CollectorStatusMessage collectorStatus,
                                                                Collection<SignatureEntry> invocationEntries) {
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

        return new ChunkedSignatureSender(sendProgress, usernames, collectorStatus, signatures);
    }

    private class ChunkedSignatureSender implements Runnable {
        private final boolean sendProgress;
        private final Collection<String> usernames;
        private final CollectorStatusMessage collectorStatus;
        private final List<List<Signature>> chunks;
        private final int progressMax;

        private int currentChunk = 0;
        private int progressValue;

        private ChunkedSignatureSender(boolean sendProgress, Collection<String> usernames, CollectorStatusMessage collectorStatus,
                                       List<Signature> signatures) {
            this.sendProgress = sendProgress;
            this.usernames = usernames;
            this.collectorStatus = collectorStatus;
            this.chunks = Lists.partition(signatures, CHUNK_SIZE);
            this.progressValue = 0;
            this.progressMax = signatures.size();
        }

        @Override
        public void run() {
            for (SignatureMessage message = nextChunk(); message != null; ) {
                for (String username : usernames) {
                    if (userHandler.isPresent(username)) {
                        log.debug("Sending {} signatures to '{}' (chunk {} of {})", message.getSignatures().size(), username,
                                  currentChunk, chunks.size());
                        messagingTemplate.convertAndSendToUser(username, "/queue/signatureUpdates", message);
                        try {
                            // Give the JavaScript client some leeway...
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            log.warn("Interrupted");
                        }
                    }
                }
            }
        }

        public SignatureMessage nextChunk() {
            if (currentChunk >= chunks.size()) {
                return null;
            }

            SignatureMessage.SignatureMessageBuilder builder = SignatureMessage.builder()
                                                                               .signatures(chunks.get(currentChunk));
            if (sendProgress && currentChunk < chunks.size() - 1) {
                builder.progress(Progress.builder().value(progressValue).max(progressMax).build());
            }
            if (currentChunk == 0) {
                builder.collectorStatus(collectorStatus);
            }

            currentChunk += 1;
            progressValue += currentChunk * CHUNK_SIZE;

            return builder.build();
        }
    }

    private CollectorStatusMessage toCollectorStatusMessage(Collection<CollectorEntry> collectors) {
        long now = System.currentTimeMillis();
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

        return CollectorStatusMessage.builder()
                                     .collectionStartedAt(DateUtils.formatDate(startedAt))
                                     .collectionAge(DateUtils.getAge(now, startedAt))
                                     .updateReceivedAt(DateUtils.formatDate(updatedAt))
                                     .updateAge(DateUtils.getAge(now, updatedAt))
                                     .collectors(collectorStrings)
                                     .build();
    }

    // --- JSON objects -----------------------------------------------------------------------------------

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
    static class SignatureMessage {
        CollectorStatusMessage collectorStatus;
        Progress progress;
        List<Signature> signatures;
    }

    @Value
    @Builder
    static class CollectorStatusMessage {
        String collectionStartedAt;
        String collectionAge;
        String updateReceivedAt;
        String updateAge;
        Collection<String> collectors;
    }

}
