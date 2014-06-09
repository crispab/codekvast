package se.crisp.duck.agent.main;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
public class AppUsage {
    private final Map<String, Long> signatureUsedAt = new HashMap<>();
    private final Set<String> notUploadedSignatures = new HashSet<>();

    public void put(String signature, long usedAtMillis) {
        if (signature != null) {
            Long oldUsage = signatureUsedAt.get(signature);

            signatureUsedAt.put(signature, usedAtMillis);

            if (oldUsage == null || usedAtMillis != oldUsage) {
                notUploadedSignatures.add(signature);
            }
        }
    }

    public Map<String, Long> getNotUploadedSignatures() {
        Map<String, Long> result = new HashMap<>();
        for (String signature : notUploadedSignatures) {
            result.put(signature, signatureUsedAt.get(signature));
        }
        return result;
    }

    public void allSignaturesAreUploaded() {
        notUploadedSignatures.clear();
    }
}
