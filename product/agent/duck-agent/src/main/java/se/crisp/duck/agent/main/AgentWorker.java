package se.crisp.duck.agent.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.agent.util.FileUtils;
import se.crisp.duck.agent.util.Usage;
import se.crisp.duck.server.agent.ServerDelegate;
import se.crisp.duck.server.agent.ServerDelegateException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * @author Olle Hallin
 */
@Component
@Slf4j
public class AgentWorker {
    private final AgentConfig config;
    private final CodeBaseScanner codeBaseScanner;
    private final ServerDelegate serverDelegate;
    private final AppUsage appUsage = new AppUsage();

    private CodeBase codeBase;
    private long usageFileModifiedAtMillis;

    @Inject
    public AgentWorker(AgentConfig config, CodeBaseScanner codeBaseScanner, ServerDelegate serverDelegate) {
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${duck.serverUploadIntervalMillis}")
    public void analyseSensorData() {
        log.debug("Analyzing sensor data");

        analyzeCodeBaseIfNeeded(new CodeBase(config));

        processUsageDataIfNew(config.getUsageFile());
    }

    private void analyzeCodeBaseIfNeeded(CodeBase newCodeBase) {
        if (!newCodeBase.equals(codeBase)) {
            newCodeBase.scanSignatures(codeBaseScanner);
            try {
                serverDelegate.uploadSignatures(newCodeBase.getSignatures());
                codeBase = newCodeBase;
            } catch (ServerDelegateException e) {
                log.error("Could not upload signatures", e);
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
            serverDelegate.uploadUsage(appUsage.getNotUploadedSignatures());
            appUsage.allSignaturesAreUploaded();
        } catch (ServerDelegateException e) {
            log.error("Could not upload usage data", e);
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

    @PreDestroy
    public void shutdownHook() {
        log.info("{} shuts down", getClass().getSimpleName());
    }

}
