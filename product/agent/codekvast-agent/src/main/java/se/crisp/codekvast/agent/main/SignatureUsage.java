package se.crisp.codekvast.agent.main;

import se.crisp.codekvast.server.agent.model.v1.UsageConfidence;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * SignatureUsage keeps track of which usage entries have not yet been uploaded to the codekvast-server.
 * It also makes sure that an older signature is not uploaded after a younger signature.
 *
 * @author Olle Hallin
 */
@NotThreadSafe
class SignatureUsage {

    private final Set<UsageDataEntry> notUploadedSignatures = new HashSet<>();
    private final Map<String, Long> ages = new HashMap<>();

    void put(String signature, long usedAtMillis, UsageConfidence confidence) {
        if (signature != null) {
            Long age = ages.get(signature);
            if (age == null || age < usedAtMillis) {
                if (age != null) {
                    removeSignature(signature);
                }
                notUploadedSignatures.add(new UsageDataEntry(signature, usedAtMillis, confidence));
                ages.put(signature, usedAtMillis);
            }
        }
    }

    private void removeSignature(String signature) {
        for (Iterator<UsageDataEntry> iterator = notUploadedSignatures.iterator(); iterator.hasNext(); ) {
            UsageDataEntry entry = iterator.next();
            if (entry.getSignature().equals(signature)) {
                iterator.remove();
            }
        }
    }

    void clearNotUploadedSignatures() {
        notUploadedSignatures.clear();
        ages.clear();
    }
}
