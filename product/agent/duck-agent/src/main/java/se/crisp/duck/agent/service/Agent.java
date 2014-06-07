package se.crisp.duck.agent.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.crisp.duck.agent.util.Configuration;
import se.crisp.duck.agent.util.SensorUtils;
import se.crisp.duck.agent.util.Usage;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

/**
 * @author Olle Hallin
 */
@RequiredArgsConstructor
@Slf4j
public class Agent extends TimerTask {
    private final Configuration config;
    private final Map<String, Long> dataFileModifiedAtMillis = new HashMap<>();
    private final CodeBaseScanner codeBaseScanner;
    private final Map<String, AppUsage> appUsages = new HashMap<>();
    private CodeBase codeBase;

    private void start() {
        Timer timer = new Timer(getClass().getSimpleName(), false);
        long intervalMillis = config.getServerUploadIntervalSeconds() * 1000L;
        timer.scheduleAtFixedRate(this, 10L, intervalMillis);

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
        log.info("Started with {}", config);
    }

    @Override
    public void run() {
        analyzeCodeBaseIfNeeded(new CodeBase(config));

        for (File usageFile : getUsageFiles()) {
            processUsageDataIfNew(usageFile);
        }
    }

    private void processUsageDataIfNew(File usageFile) {
        long modifiedAt = usageFile.lastModified();
        Long oldModifiedAt = dataFileModifiedAtMillis.get(usageFile.getPath());
        if (oldModifiedAt == null || oldModifiedAt != modifiedAt) {
            String appName = getAppName(usageFile);

            applyRecordedUsage(codeBase, getAppUsage(appName), SensorUtils.readUsageFrom(usageFile));

            // TODO: post sensorRun and usages to data warehouse

            dataFileModifiedAtMillis.put(usageFile.getPath(), modifiedAt);
        }
    }

    private AppUsage getAppUsage(String appName) {
        AppUsage result = appUsages.get(appName);
        if (result == null) {
            result = new AppUsage(appName);
            appUsages.put(appName, result);
        }
        return result;
    }

    private String getAppName(File usageFile) {
        String name = usageFile.getName();
        return name.substring(0, name.length() - Configuration.USAGE_FILE_SUFFIX.length());
    }

    int applyRecordedUsage(CodeBase codeBase, AppUsage appUsage, List<Usage> usages) {
        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Usage usage : usages) {
            String rawSignature = usage.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);

            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
            } else {
                String baseSignature = codeBase.getBaseSignature(normalizedSignature);
                if (baseSignature != null) {
                    log.debug("{} replaced by {}", normalizedSignature, baseSignature);

                    overridden += 1;
                    normalizedSignature = codeBase.normalizeSignature(baseSignature);
                } else if (normalizedSignature.equals(rawSignature)) {
                    unrecognized += 1;
                    log.warn("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    log.warn("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            appUsage.put(normalizedSignature, usage.getUsedAtMillis());
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored signature usages applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.info("{} signature usages applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
        return unrecognized;
    }

    private void analyzeCodeBaseIfNeeded(CodeBase newCodeBase) {
        if (!newCodeBase.equals(codeBase)) {
            newCodeBase.initSignatures(codeBaseScanner);
            codeBase = newCodeBase;
        }
    }

    private List<File> getUsageFiles() {
        List<File> result = new ArrayList<>();
        File[] usageFiles = config.getSensorsPath().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Configuration.USAGE_FILE_SUFFIX);
            }
        });

        if (usageFiles != null) {
            for (File file : usageFiles) {
                result.add(file);
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
