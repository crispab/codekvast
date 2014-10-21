package se.crisp.codekvast.agent.main;

import lombok.Getter;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds a map of signature usage data entries. It keeps track of which entries have not yet been uploaded to the codekvast-server.
 *
 * @author Olle Hallin
 */
class SignatureUsage {

    private final Map<String, UsageDataEntry> entries = new HashMap<>();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Getter
    private final Set<UsageDataEntry> notUploadedSignatures = new HashSet<>();

    void put(String signature, long usedAtMillis, int confidence) {
        if (signature != null) {
            UsageDataEntry newEntry = new UsageDataEntry(signature, usedAtMillis, confidence);
            UsageDataEntry oldEntry = entries.put(signature, newEntry);

            if (!newEntry.equals(oldEntry)) {
                notUploadedSignatures.add(newEntry);
            }
        }
    }

    void clearNotUploadedSignatures() {
        notUploadedSignatures.clear();
    }
}
