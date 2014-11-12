package se.crisp.codekvast.agent.main;

import lombok.Getter;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.SignatureConfidence;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * InvocationsCollector keeps track of which invocations have not yet been uploaded to the codekvast-server.
 * It also makes sure that an older invocation is not uploaded after a younger invocation.
 *
 * @author Olle Hallin
 */
@NotThreadSafe
class InvocationsCollector {

    @Getter
    private final Set<InvocationEntry> notUploadedInvocations = new HashSet<>();
    private final Map<String, Long> ages = new HashMap<>();

    void put(String signature, long invokedAtMillis, SignatureConfidence confidence) {
        if (signature != null) {
            Long age = ages.get(signature);
            if (age == null || age < invokedAtMillis) {
                if (age != null) {
                    removeSignature(signature);
                }
                notUploadedInvocations.add(new InvocationEntry(signature, invokedAtMillis, confidence));
                ages.put(signature, invokedAtMillis);
            }
        }
    }

    private void removeSignature(String signature) {
        for (Iterator<InvocationEntry> iterator = notUploadedInvocations.iterator(); iterator.hasNext(); ) {
            InvocationEntry entry = iterator.next();
            if (entry.getSignature().equals(signature)) {
                iterator.remove();
            }
        }
    }

    void clearNotUploadedSignatures() {
        notUploadedInvocations.clear();
        ages.clear();
    }
}
