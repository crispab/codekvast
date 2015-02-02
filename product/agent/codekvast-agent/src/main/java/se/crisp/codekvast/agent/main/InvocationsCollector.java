package se.crisp.codekvast.agent.main;

import lombok.Getter;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * InvocationsCollector keeps track of which invocations have not yet been uploaded to the codekvast-server.
 * It also makes sure that an older invocation is not uploaded after a younger invocation.
 *
 * @author Olle Hallin
 */
@NotThreadSafe
class InvocationsCollector {

    @Getter
    private final Set<SignatureEntry> notUploadedInvocations = new HashSet<>();
    private final Map<String, Long> ages = new HashMap<>();

    void put(String signature, long invokedAtMillis, SignatureConfidence confidence) {
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }

        if (invokedAtMillis < 0L) {
            throw new IllegalArgumentException("invokedAtMillis cannot be negative");
        }

        Long age = ages.get(signature);
        if (age == null || age <= invokedAtMillis) {
            SignatureEntry signatureEntry = new SignatureEntry(signature, invokedAtMillis, confidence);
            // Replace it. Must remove first or, else the add is a no-op.
            notUploadedInvocations.remove(signatureEntry);
            notUploadedInvocations.add(signatureEntry);
            ages.put(signature, invokedAtMillis);
        }
    }

    void clearNotUploadedSignatures() {
        notUploadedInvocations.clear();
        ages.clear();
    }
}
