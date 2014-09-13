package se.crisp.codekvast.agent.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.util.AgentConfig;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.agent.util.Sensor;
import se.crisp.codekvast.agent.util.Usage;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This is the meat of the codekvast-agent. It contains a scheduled method that uploads changed data to the codekvast-server.
 *
 * @author Olle Hallin
 */
@Component
@Slf4j
public class AgentWorker {
    private final AgentConfig config;
    private final CodeBaseScanner codeBaseScanner;
    private final ServerDelegate serverDelegate;
    private final AppUsage appUsage = new AppUsage();

    private Sensor sensor;
    private CodeBase codeBase;
    private long usageFileModifiedAtMillis;

    @Inject
    public AgentWorker(AgentConfig config, CodeBaseScanner codeBaseScanner, ServerDelegate serverDelegate) {
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.serverUploadIntervalMillis}")
    public void analyseSensorData() {
        log.debug("Analyzing sensor data");

        uploadSensorDataIfNew(config.getSensorFile());

        analyzeCodeBaseIfNeeded(new CodeBase(config));

        if (codeBase != null) {
            processUsageDataIfNew(config.getUsageFile());
        }
    }

    private void uploadSensorDataIfNew(File sensorFile) {
        try {
            Sensor newSensor = Sensor.readFrom(sensorFile);
            if (!newSensor.equals(sensor)) {
                serverDelegate.uploadSensorData(newSensor.getHostName(),
                                                newSensor.getStartedAtMillis(),
                                                newSensor.getDumpedAtMillis(),
                                                newSensor.getUuid());
                sensor = newSensor;
            }
        } catch (IOException e) {
            logException("Cannot read " + sensorFile, e);
        } catch (ServerDelegateException e) {
            logException("Cannot upload sensor data", e);
        }
    }

    private void logException(String msg, Exception e) {
        if (log.isDebugEnabled()) {
            // log with full stack trace
            log.error(msg, e);
        } else {
            // log a one-liner with the root cause
            log.error("{}: {}", msg, getRootCause(e).toString());
        }
    }

    private void analyzeCodeBaseIfNeeded(CodeBase newCodeBase) {
        if (!newCodeBase.equals(codeBase)) {
            newCodeBase.scanSignatures(codeBaseScanner);
            try {
                serverDelegate.uploadSignatureData(newCodeBase.getSignatures());
                codeBase = newCodeBase;
            } catch (ServerDelegateException e) {
                logException("Cannot upload signature data", e);
            }
        }
    }

    private void processUsageDataIfNew(File usageFile) {
        long modifiedAt = usageFile.lastModified();
        if (modifiedAt != usageFileModifiedAtMillis) {
            applyRecordedUsage(codeBase, appUsage, FileUtils.readUsageDataFrom(usageFile));
            uploadUsedSignatures(appUsage);
            usageFileModifiedAtMillis = modifiedAt;
        }
    }

    private void uploadUsedSignatures(AppUsage appUsage) {
        try {
            serverDelegate.uploadUsageData(appUsage.getNotUploadedSignatures());
            appUsage.allSignaturesAreUploaded();
        } catch (ServerDelegateException e) {
            logException("Cannot upload usage data", e);
        }
    }

    int applyRecordedUsage(CodeBase codeBase, AppUsage appUsage, List<Usage> usages) {
        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Usage usage : usages) {
            String rawSignature = usage.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);

            int confidence = -1;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                confidence = UsageDataEntry.CONFIDENCE_EXACT_MATCH;
            } else {
                String baseSignature = codeBase.getBaseSignature(normalizedSignature);
                if (baseSignature != null) {
                    log.debug("{} replaced by {}", normalizedSignature, baseSignature);

                    overridden += 1;
                    confidence = UsageDataEntry.CONFIDENCE_FOUND_IN_PARENT_CLASS;
                    normalizedSignature = baseSignature;
                } else if (normalizedSignature.equals(rawSignature)) {
                    unrecognized += 1;
                    confidence = UsageDataEntry.CONFIDENCE_NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    confidence = UsageDataEntry.CONFIDENCE_NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            appUsage.put(normalizedSignature, usage.getUsedAtMillis(), confidence);
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored signature usages applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.info("{} signature usages applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
        return unrecognized;
    }

    @PreDestroy
    public void shutdownHook() {
        log.info("{} shuts down", getClass().getSimpleName());
    }

    private Throwable getRootCause(Throwable t) {
        return t.getCause() == null ? t : getRootCause(t.getCause());
    }
}
