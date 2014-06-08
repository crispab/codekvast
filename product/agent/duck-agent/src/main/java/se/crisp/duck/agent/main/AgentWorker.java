package se.crisp.duck.agent.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.agent.util.SensorUtils;
import se.crisp.duck.agent.util.Usage;
import se.crisp.duck.server.agent.ServerDelegate;
import se.crisp.duck.server.agent.ServerDelegateException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * @author Olle Hallin
 */
@Component
@Slf4j
public class AgentWorker {
    private final AgentConfig config;
    private final Map<String, Long> dataFileModifiedAtMillis = new HashMap<>();
    private final CodeBaseScanner codeBaseScanner;
    private final ServerDelegate serverDelegate;
    private final Map<String, AppUsage> appUsages = new HashMap<>();

    private CodeBase codeBase;

    @Inject
    public AgentWorker(AgentConfig config, CodeBaseScanner codeBaseScanner, ServerDelegate serverDelegate) {
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${duck.serverUploadIntervalMillis}")
    public void uploadUsageDataToServer() {
        log.debug("Uploading data to server");
        analyzeCodeBaseIfNeeded(new CodeBase(config));

        for (File usageFile : getUsageFiles()) {
            processUsageDataIfNew(usageFile);
        }
    }

    private void processUsageDataIfNew(File usageFile) {
        long modifiedAt = usageFile.lastModified();
        Long oldModifiedAt = dataFileModifiedAtMillis.get(usageFile.getPath());
        if (oldModifiedAt == null || oldModifiedAt != modifiedAt) {
            AppUsage appUsage = getAppUsage(getAppName(usageFile));

            applyRecordedUsage(codeBase, appUsage, SensorUtils.readUsageFrom(usageFile));

            uploadUsedSignatures(appUsage);

            dataFileModifiedAtMillis.put(usageFile.getPath(), modifiedAt);
        }
    }

    private void uploadUsedSignatures(AppUsage appUsage) {
        int count = 0;
        for (Map.Entry<String, Long> entry : appUsage.getNotUploadedSignatures().entrySet()) {
            // TODO: convert to server format
            count += 1;
        }

        log.info("Uploading {} new usages to {}", count, config.getServerUri());
        // TODO: upload to server

        appUsage.allSignaturesAreUploaded();
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
        return name.substring(0, name.length() - AgentConfig.USAGE_FILE_SUFFIX.length());
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
            try {
                uploadSignatures(newCodeBase);
                codeBase = newCodeBase;
            } catch (ServerDelegateException e) {
                log.error("Could not upload signatures", e);
            }
        }
    }

    private void uploadSignatures(CodeBase codeBase) throws ServerDelegateException {
        Collection<String> signatures = codeBase.getSignatures();
        if (signatures.size() > 0) {
            log.info("Uploading {} signatures for {} to {}", signatures.size(), codeBase, config.getServerUri());
            serverDelegate.uploadSignatures(signatures);
        }
    }

    private List<File> getUsageFiles() {
        List<File> result = new ArrayList<>();
        File[] usageFiles = config.getSensorsPath().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(AgentConfig.USAGE_FILE_SUFFIX);
            }
        });

        if (usageFiles != null) {
            Collections.addAll(result, usageFiles);
        }

        return result;
    }

    @PreDestroy
    public void shuttingDown() {
        log.info("Shuts down");
    }

}
