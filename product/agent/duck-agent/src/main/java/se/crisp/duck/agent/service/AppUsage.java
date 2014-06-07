package se.crisp.duck.agent.service;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olle Hallin
 */
@Value
public class AppUsage {
    private final String appName;
    private final Map<String, Long> signatureUsedAt = new HashMap<>();

    public void put(String signature, long usedAtMillis) {
        if (signature != null) {
            signatureUsedAt.put(signature, usedAtMillis);
        }
    }
}
