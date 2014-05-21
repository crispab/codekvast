package duck.spike.agent;


import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.Usage;
import duck.spike.util.UsageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
@Slf4j
public class Agent extends TimerTask {
    private final Configuration config;
    private final Map<String, Long> codeBase = new TreeMap<>();
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

        importCodeBaseIfNew(sensorRun);
        processUsageDataIfNew(config.getDataFile());
    }

    private void processUsageDataIfNew(File dataFile) {
        long modifiedAt = dataFile.lastModified();
        if (modifiedAt != dataFileModifiedAtMillis) {
            applyLatestSensorDataToCodeBase(config.getDataFile());
            logStatistics();

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis = modifiedAt;
        }
    }

    int applyLatestSensorDataToCodeBase(File dataFile) {
        List<Usage> usages = UsageUtils.readFromFile(dataFile);
        int recognized = 0;
        int unrecognized = 0;
        for (Usage usage : usages) {
            if (codeBase.put(usage.getSignature(), usage.getUsedAtMillis()) == null) {
                log.warn("Unrecognized runtime signature: {}", usage.getSignature());
                unrecognized += 1;
            } else {
                recognized += 1;
            }
        }
        log.info("{} recognized and {} unrecognized signatures found", recognized, unrecognized);
        return unrecognized;
    }

    private void logStatistics() {
        int unused = 0;
        int used = 0;

        for (Long usedAtMillis : codeBase.values()) {
            if (usedAtMillis == 0L) {
                unused += 1;
            } else {
                used += 1;
            }
        }
        log.info("Posting usage data for {} unused and {} used methods in {} to {}", unused, used,
                 config.getAppName(), config.getWarehouseUri());
    }

    private void importCodeBaseIfNew(SensorRun sensorRun) {
        if (lastSeenSensorUUID == null || !lastSeenSensorUUID.equals(sensorRun.getUuid())) {
            log.debug("Scanning code base at {}", config.getCodeBaseUri());
            prepareCodeBase(codeBaseScanner.getPublicMethodSignatures(config));
            UsageUtils.dumpUsageData(config.getCodeBaseFile(), 0, codeBase);
            lastSeenSensorUUID = sensorRun.getUuid();
        }
    }

    void prepareCodeBase(List<String> signatures) {
        codeBase.clear();
        for (String signature : signatures) {
            codeBase.put(signature, 0L);
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
