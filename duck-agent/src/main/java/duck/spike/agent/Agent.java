package duck.spike.agent;


import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.Usage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
@Slf4j
public class Agent extends TimerTask {
    private final Configuration config;
    private final Map<String, Usage> usages = new HashMap<>();

    private long dataFileModifiedAtMillis;
    private UUID lastSeenSensorUUID;

    private void start() {
        Timer timer = new Timer(getClass().getSimpleName(), false);
        long intervalMillis = config.getWarehouseUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, 10L, intervalMillis);
        log.info("Started with {}", config);
    }

    @Override
    public void run() {
        SensorRun sensorRun = scanClasspathIfNewSensorRun();
        if (sensorRun == null) {
            log.info("{} not found", config.getSensorFile());
            return;
        }

        long modifiedAt = config.getDataFile().lastModified();
        if (modifiedAt != dataFileModifiedAtMillis) {
            // Overwrite with the latest usages
            usages.putAll(Usage.readUsagesFromFile(config.getDataFile()));

            int unused = 0;
            int used = 0;

            for (Usage usage : usages.values()) {
                if (usage.getUsedAtMillis() == 0L) {
                    unused += 1;
                }
                if (usage.getUsedAtMillis() != 0L) {
                    used += 1;
                }
            }

            log.info("Posting usage data for {} unused and {} used methods in {} to {}", unused, used,
                     config.getAppName(), config.getWarehouseUri());

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis = modifiedAt;
        }
    }

    private SensorRun scanClasspathIfNewSensorRun() {
        SensorRun sensorRun;
        try {
            sensorRun = SensorRun.readFrom(config.getSensorFile());
        } catch (IOException e) {
            // Cannot read sensor file, do nothing now...
            return null;
        }

        if (lastSeenSensorUUID == null || !lastSeenSensorUUID.equals(sensorRun.getUuid())) {
            log.debug("Scanning code base at {}", config.getCodeBaseUri());
            usages.clear();
            usages.putAll(new CodeBaseScanner(config).scanCodeBase());
            lastSeenSensorUUID = sensorRun.getUuid();
        }
        return sensorRun;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Usage: agent <path/to/duck.properties>");
            System.exit(1);
        }

        try {
            Configuration config = Configuration.parseConfigFile(args[0]);
            new Agent(config).start();
        } catch (Exception e) {
            log.error("Cannot start agent", e);
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

}
