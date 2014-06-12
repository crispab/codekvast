package se.crisp.duck.agent.main;

import lombok.Getter;
import se.crisp.duck.server.agent.model.v1.UsageDataEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Olle Hallin
 */
class AppUsage {

    private final Map<String, UsageDataEntry> entries = new HashMap<>();

    @Getter
    private final Set<UsageDataEntry> notUploadedSignatures = new HashSet<>();

    void put(String signature, long usedAtMillis, int confidence) {
        if (signature != null) {
            UsageDataEntry oldEntry = entries.get(signature);
            UsageDataEntry newEntry = new UsageDataEntry(signature, usedAtMillis, confidence);

            entries.put(signature, newEntry);

            if (!newEntry.equals(oldEntry)) {
                notUploadedSignatures.add(newEntry);
            }
        }
    }

    void allSignaturesAreUploaded() {
        notUploadedSignatures.clear();
    }
}
