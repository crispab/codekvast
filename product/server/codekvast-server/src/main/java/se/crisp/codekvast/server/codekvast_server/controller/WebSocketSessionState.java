package se.crisp.codekvast.server.codekvast_server.controller;

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
    private static final int CHUNK_SIZE = 100;
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

    public void setSignatures(Collection<SignatureEntry> signatures) {
        this.signatures.addAll(signatures);
        this.progressMax = signatures.size();
        this.offset = 0;
    }

    public SignatureHandler.SignaturesAvailableMessage getSignaturesAvailableMessage() {
        return SignatureHandler.SignaturesAvailableMessage.builder()
                                                          .pendingSignatures(signatures.size())
                                                          .progress(SignatureHandler.Progress.builder().value(offset + 1).max(progressMax)
                                                                                             .build())
                                                          .build();
    }

    public SignatureHandler.SignatureDataMessage getNextSignatureDataMessage() {
        int last = Math.min(offset + CHUNK_SIZE, signatures.size());

        SignatureHandler.SignatureDataMessage.SignatureDataMessageBuilder builder = SignatureHandler.SignatureDataMessage
                .builder()
                .progress(SignatureHandler.Progress.builder().value(offset + 1).max(progressMax).build());

        List<SignatureHandler.Signature> sig = new ArrayList<>();
        for (SignatureEntry entry : signatures.subList(offset, last)) {

            sig.add(SignatureHandler.Signature.builder()
                                              .name(entry.getSignature())
                                              .invokedAtMillis(entry.getInvokedAtMillis())
                                              .invokedAtString(DateUtils.formatDate(entry.getInvokedAtMillis()))
                                              .build());
        }
        builder.signatures(sig);

        offset += last;
        boolean more = offset < signatures.size();
        if (!more) {
            signatures.clear();
        }
        builder.more(more);
        return builder.build();
    }
}
