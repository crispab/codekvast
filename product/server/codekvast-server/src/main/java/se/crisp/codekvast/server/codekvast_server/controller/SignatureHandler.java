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
import se.crisp.codekvast.server.codekvast_server.dao.CollectorTimestamp;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorUptimeEvent;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataUpdatedEvent;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;
import se.crisp.codekvast.server.codekvast_server.util.DateUtils;

import javax.inject.Inject;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public void onCollectorUptimeEvent(CollectorUptimeEvent event) {
        long now = System.currentTimeMillis();
        Timestamp timestamp = toStompTimestamp(now, event.getCollectorTimestamp());

        for (String username : event.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} to '{}'", timestamp, username);
                messagingTemplate.convertAndSendToUser(username, "/queue/timestamps", timestamp);
            }
        }
    }

    @Subscribe
    public void onInvocationDataUpdatedEvent(InvocationDataUpdatedEvent event) throws CodekvastException {
        SignatureData sig = toSignatureData(event.getInvocationEntries());
        for (String username : event.getUsernames()) {
            if (userHandler.isPresent(username)) {
                log.debug("Sending {} signatures and {} packages to '{}'", sig.getSignatures().size(), sig.getPackages().size(), username);
                messagingTemplate.convertAndSendToUser(username, "/queue/signatureUpdates", sig);
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

        SignatureData sig = toSignatureData(userService.getSignatures(username));
        log.debug("Sending {} signatures and {} packages to '{}'", sig.getSignatures().size(), sig.getPackages().size(), username);
        return sig;
    }

    private SignatureData toSignatureData(Collection<SignatureEntry> invocationEntries) {
        long now = System.currentTimeMillis();

        List<Signature> signatures = new ArrayList<>();
        Set<String> packages = new TreeSet<>();
        Pattern pkgPattern = Pattern.compile("([\\p{javaLowerCase}\\.]+)\\.");

        for (SignatureEntry entry : invocationEntries) {
            String s = entry.getSignature();

            Matcher m = pkgPattern.matcher(s);
            if (m.find()) {
                String pkg = m.group(1);
                packages.add(pkg);
            }

            long invokedAtMillis = entry.getInvokedAtMillis();

            signatures.add(Signature.builder()
                                    .name(s)
                                    .invokedAtMillis(invokedAtMillis)
                                    .invokedAtString(DateUtils.formatDate(invokedAtMillis))
                                    .age(DateUtils.getAge(now, invokedAtMillis))
                                    .build());
        }

        // Add the immediate parent packages, or else it will be impossible to reset the Packages filter without
        // a lot of Angular code...
        addParentPackages(packages);

        return SignatureData.builder()
                            .signatures(signatures)
                            .packages(packages)
                            .build();
    }

    private Timestamp toStompTimestamp(long now, CollectorTimestamp timestamp) {
        return Timestamp.builder()
                        .collectionStartedAt(DateUtils.formatDate(timestamp.getStartedAtMillis()))
                        .collectionAge(DateUtils.getAge(now, timestamp.getStartedAtMillis()))
                        .updateReceivedAt(DateUtils.formatDate(timestamp.getDumpedAtMillis()))
                        .updateAge(DateUtils.getAge(now, timestamp.getDumpedAtMillis())).build();
    }

    @Value
    @Builder
    static class SignatureData {
        @NonNull
        private final List<Signature> signatures;
        @NonNull
        private final Set<String> packages;
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
    }

    @Value
    @Builder
    static class Signature {
        @NonNull
        private final String name;
        private final long invokedAtMillis;
        @NonNull
        private final String invokedAtString;
        @NonNull
        private final String age;
    }

    static void addParentPackages(Set<String> packages) {
        Set<String> parentPackages = new HashSet<>();
        for (String pkg : packages) {
            int dot = pkg.indexOf('.');
            while (dot > 0) {
                String parentPkg = pkg.substring(0, dot);
                parentPackages.add(parentPkg);
                dot = pkg.indexOf('.', dot + 1);
            }
        }
        packages.addAll(parentPackages);
        packages.add("");
    }

}
