package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.util.DateUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */ //--- Helper class for maintaining state per web socket session
@Component
@Scope(value = "websocket", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
public class WebSocketSessionState {
    private List<SignatureEntry> signatures = new ArrayList<>();
    private int offset;
    private int progressMax;

    @PostConstruct
    public void init() {
        log.debug("Web socket session started");
    }

    @PreDestroy
    public void destroy() {
        log.debug("Web socket session terminated");
    }

    public SignaturesAvailableMessage setSignatures(Collection<SignatureEntry> signatures) {
        this.signatures.addAll(signatures);
        this.progressMax = signatures.size();
        this.offset = 0;

        SignaturesAvailableMessage result = SignaturesAvailableMessage.builder()
                                                                      .pendingSignatures(signatures.size())
                                                                      .progress(Progress.builder().value(0).max(progressMax).build())
                                                                      .build();
        return result;
    }

    public SignatureDataMessage getNextSignatureDataMessage(int chunkSize) {
        int last = Math.min(offset + chunkSize, signatures.size());

        SignatureDataMessage.SignatureDataMessageBuilder builder = SignatureDataMessage
                .builder()
                .progress(Progress.builder().value(offset).max(progressMax).build());

        List<Signature> sig = new ArrayList<>();
        for (SignatureEntry entry : signatures.subList(offset, last)) {

            sig.add(Signature.builder()
                             .name(entry.getSignature())
                             .invokedAtMillis(entry.getInvokedAtMillis())
                             .invokedAtString(DateUtils.formatDate(entry.getInvokedAtMillis()))
                             .build());
        }
        builder.signatures(sig);

        offset += chunkSize;
        boolean more = offset < signatures.size();
        if (!more) {
            signatures.clear();
        }
        builder.more(more);
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
}
