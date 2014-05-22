package duck.spike.agent;


import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.SensorUtils;
import duck.spike.util.Usage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
@Slf4j
public class Agent extends TimerTask {
    private final Configuration config;
    private final Map<String, Long> signatureUsage = new TreeMap<>();
    private final CodeBaseScanner codeBaseScanner;

    private long dataFileModifiedAtMillis;
    private UUID lastSeenSensorUUID;

    private void start() {
        Timer timer = new Timer(getClass().getSimpleName(), false);
        long intervalMillis = config.getWarehouseUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, 10L, intervalMillis);

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
        log.info("Started with {}", config);
    }

    @Override
    public void run() {
        SensorRun sensorRun = getLatestSensorRun();
        if (sensorRun == null) {
            log.info("Waiting for {} to start", config.getAppName());
            return;
        }

        importSignaturesIfNew(sensorRun);
        processUsageDataIfNew(config.getDataFile());
    }

    private void processUsageDataIfNew(File dataFile) {
        long modifiedAt = dataFile.lastModified();
        if (modifiedAt != dataFileModifiedAtMillis) {
            applyRecordedUsage(SensorUtils.readUsageFrom(config.getDataFile()));
            logStatistics();

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis = modifiedAt;
        }
    }

    int applyRecordedUsage(List<Usage> usages) {
        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;

        for (Usage usage : usages) {
            String rawSignature = usage.getSignature();
            String normalizedSignature = normalizeSignature(rawSignature);

            if (normalizedSignature == null) {
                ignored += 1;
            } else if (!signatureUsage.containsKey(normalizedSignature)) {
                if (normalizedSignature.equals(rawSignature)) {
                    log.warn("Unrecognized normalized signature: {}", normalizedSignature);
                } else {
                    log.warn("Unrecognized normalized signature: {} (was {})", normalizedSignature, rawSignature);
                }
                unrecognized += 1;
            } else {
                recognized += 1;
            }

            if (normalizedSignature != null) {
                signatureUsage.put(normalizedSignature, usage.getUsedAtMillis());
            }
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} unrecognized and {} ignored signature usages applied", recognized, unrecognized, ignored);
        } else {
            log.info("{} signature usages applied ({} ignored)", recognized, ignored);
        }
        return unrecognized;
    }

    private final Pattern[] enhanceByGuicePatterns = {
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.getIndex\\(java\\.lang\\.Class\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.newInstance\\(int, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.invoke\\(int, java\\.lang\\.Object, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\(com\\.google\\.inject\\.internal\\.cglib.*\\)$"),
    };

    String normalizeSignature(String signature) {
        for (Pattern pattern : enhanceByGuicePatterns) {
            if (pattern.matcher(signature).matches()) {
                return null;
            }
        }
        return signature.replaceAll(" final ", " ").replaceAll("\\.\\.EnhancerByGuice\\.\\..*[0-9a-f]\\.([\\w]+\\()", ".$1");
    }

    private void logStatistics() {
        int unused = 0;
        int used = 0;

        for (Long usedAtMillis : signatureUsage.values()) {
            if (usedAtMillis == 0L) {
                unused += 1;
            } else {
                used += 1;
            }
        }
        log.info("Posting usage data for {} unused and {} used methods in {} to {}", unused, used,
                 config.getAppName(), config.getWarehouseUri());
    }

    private void importSignaturesIfNew(SensorRun sensorRun) {
        if (lastSeenSensorUUID == null || !lastSeenSensorUUID.equals(sensorRun.getUuid())) {
            Set<String> signatures = codeBaseScanner.getPublicMethodSignatures(config);

            resetSignatureUsage(signatures);
            writeSignaturesTo(signatures, config.getSignatureFile());

            lastSeenSensorUUID = sensorRun.getUuid();
        }
    }

    private void writeSignaturesTo(Set<String> signatures, File file) {
        PrintStream out = null;
        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));

            for (String signature : signatures) {
                out.println(signature);
            }

            if (!tmpFile.renameTo(file)) {
                log.error("Cannot rename {} to {}", tmpFile.getAbsolutePath(), file.getAbsolutePath());
                tmpFile.delete();
            }
        } catch (IOException e) {
            log.error("Cannot create " + file, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    void resetSignatureUsage(Set<String> signatures) {
        signatureUsage.clear();
        for (String signature : signatures) {
            signatureUsage.put(normalizeSignature(signature), 0L);
        }
    }

    private SensorRun getLatestSensorRun() {
        try {
            return SensorRun.readFrom(config.getSensorFile());
        } catch (IOException e) {
            // Cannot read sensor file, do nothing now...
            return null;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Usage: agent <path/to/duck.properties>");
            System.exit(1);
        }

        try {
            Configuration config = Configuration.parseConfigFile(args[0]);
            new Agent(config, new CodeBaseScanner()).start();
        } catch (Exception e) {
            log.error("Cannot start agent", e);
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private class ShutdownHook implements Runnable {
        @Override
        public void run() {
            log.info("Shutting down");
        }
    }
}
