package se.crisp.duck.agent.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.crisp.duck.agent.util.Configuration;
import se.crisp.duck.agent.util.Sensor;
import se.crisp.duck.agent.util.SensorUtils;
import se.crisp.duck.agent.util.Usage;

import java.io.*;
import java.net.URL;
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
    private final Map<String, String> baseSignatures = new HashMap<>();
    private final CodeBaseScanner codeBaseScanner;

    private long dataFileModifiedAtMillis;
    private CodeBase codeBase;

    private final Pattern[] enhanceByGuicePatterns = {
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.getIndex\\(java\\.lang\\.Class\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.newInstance\\(int, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\.\\.FastClassByGuice.*\\.invoke\\(int, java\\.lang\\.Object, java\\.lang\\.Object\\[\\]\\)$"),
            Pattern.compile(".*\\(com\\.google\\.inject\\.internal\\.cglib.*\\)$"),
    };

    private void start() {
        Timer timer = new Timer(getClass().getSimpleName(), false);
        long intervalMillis = config.getServerUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, 10L, intervalMillis);

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
        log.info("Started with {}", config);
    }

    @Override
    public void run() {
        List<Sensor> sensors = getRunningSensors();
        if (sensors.isEmpty()) {
            log.debug("Waiting some sensor to start");
            return;
        }

        importSignaturesIfNeeded(new CodeBase(config.getCodeBaseUri().getPath()));
        processUsageDataIfNew(config.getUsageFile());
    }

    private void processUsageDataIfNew(File dataFile) {
        long modifiedAt = dataFile.lastModified();
        if (modifiedAt != dataFileModifiedAtMillis) {
            applyRecordedUsage(SensorUtils.readUsageFrom(config.getUsageFile()));
            logStatistics();

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis = modifiedAt;
        }
    }

    int applyRecordedUsage(List<Usage> usages) {
        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Usage usage : usages) {
            String rawSignature = usage.getSignature();
            String normalizedSignature = normalizeSignature(rawSignature);

            if (normalizedSignature == null) {
                ignored += 1;
            } else if (signatureUsage.containsKey(normalizedSignature)) {
                recognized += 1;
            } else {
                String baseSignature = baseSignatures.get(normalizedSignature);
                if (baseSignature != null) {
                    log.debug("{} replaced by {}", normalizedSignature, baseSignature);

                    overridden += 1;
                    normalizedSignature = normalizeSignature(baseSignature);
                } else if (normalizedSignature.equals(rawSignature)) {
                    unrecognized += 1;
                    log.warn("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    log.warn("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            if (normalizedSignature != null) {
                signatureUsage.put(normalizedSignature, usage.getUsedAtMillis());
            }
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored signature usages applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.info("{} signature usages applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
        return unrecognized;
    }

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
                 config.getAppName(), config.getServerUri());
    }

    private void importSignaturesIfNeeded(CodeBase newCodeBase) {
        if (!newCodeBase.equals(codeBase)) {
            CodeBaseScanner.Result scannerResult = codeBaseScanner.getPublicMethodSignatures(config, newCodeBase);

            resetSignatureUsage(scannerResult);
            writeSignaturesTo(scannerResult, config.getSignatureFile());

            codeBase = newCodeBase;
        }
    }

    private void writeSignaturesTo(CodeBaseScanner.Result scannerResult, File file) {
        PrintStream out = null;
        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));

            out.println("# Signatures:");
            for (String signature : scannerResult.signatures) {
                out.println(signature);
            }

            out.println();
            out.println("------------------------------------------------------------------------------------------------");
            out.println("# Overridden signatures:");
            out.println("# child() -> base()");
            for (Map.Entry<String, String> entry : scannerResult.overriddenSignatures.entrySet()) {
                out.printf("%s -> %s%n", entry.getKey(), entry.getValue());
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

    void resetSignatureUsage(CodeBaseScanner.Result result) {
        signatureUsage.clear();
        for (String signature : result.signatures) {
            signatureUsage.put(normalizeSignature(signature), 0L);
        }
        baseSignatures.clear();
        baseSignatures.putAll(result.overriddenSignatures);
    }

    private List<Sensor> getRunningSensors() {
        List<Sensor> result = new ArrayList<>();

        File[] sensorFiles = config.getSensorsPath().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Configuration.SENSOR_FILE_SUFFIX);
            }
        });

        if (sensorFiles != null) {
            for (File file : sensorFiles) {
                try {
                    result.add(Sensor.readFrom(file));
                } catch (IOException e) {
                    log.warn("Cannot read {}: {}", file, e.toString());
                }
            }
        }

        return result;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        try {
            Configuration config = Configuration.parseConfigFile(locateConfigFile(args));
            new Agent(config, new CodeBaseScanner()).start();
        } catch (Exception e) {
            log.error("Cannot start agent", e);
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static String locateConfigFile(String[] args) {
        String result = args == null || args.length < 1 ? null : args[0];
        if (result == null) {
            URL url = Agent.class.getResource("/duck.properties");
            if (url != null) {
                result = url.getFile();
            }
        }

        if (result == null) {
            System.err.println("Cannot locate duck.properties.\nEither put it in conf/ or specify the path on the command line");
            System.exit(1);
        }
        return result;
    }

    private class ShutdownHook implements Runnable {
        @Override
        public void run() {
            log.info("Shutting down");
        }
    }
}
