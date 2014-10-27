package se.crisp.codekvast.agent.main;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.util.AgentConfig;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.agent.util.JvmRun;
import se.crisp.codekvast.agent.util.Usage;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.v1.UsageConfidence;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
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
    private final SignatureUsage signatureUsage = new SignatureUsage();
    private final String codekvastGradleVersion;
    private final String codekvastVcsId;

    private JvmRun jvmRun;

    @Setter(AccessLevel.MODULE)
    private CodeBase codeBase;

    @Inject
    public AgentWorker(AgentConfig config, CodeBaseScanner codeBaseScanner, ServerDelegate serverDelegate,
                       @Value("@{info.build.gradle.version}") String codekvastGradleVersion,
                       @Value("@{info.build.git.id}") String codekvastVcsId) {
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
        this.codekvastGradleVersion = codekvastGradleVersion;
        this.codekvastVcsId = codekvastVcsId;

        // The agent might have crashed between consuming usage data files and uploading them to the server.
        // Make sure that usage data is not lost...
        FileUtils.resetAllConsumedUsageDataFiles(config.getUsageFile());
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.serverUploadIntervalMillis}")
    public void analyseCollectorData() {
        log.debug("Analyzing collector data");

        uploadJvmRunIfNeeded();

        analyzeAndUploadCodeBaseIfNeeded(new CodeBase(config));

        processUsageDataIfNeeded();
    }

    private void uploadJvmRunIfNeeded() {
        File jvmRunFile = config.getJvmRunFile();
        try {
            JvmRun newJvmRun = JvmRun.readFrom(jvmRunFile);
            if (!newJvmRun.equals(jvmRun)) {
                serverDelegate.uploadJvmRunData(
                        config.getAppName(),
                        config.getAppVersion(),
                        newJvmRun.getHostName(),
                        newJvmRun.getStartedAtMillis(),
                        newJvmRun.getDumpedAtMillis(),
                        newJvmRun.getJvmFingerprint(),
                        codekvastGradleVersion,
                        codekvastVcsId);
                jvmRun = newJvmRun;
            }
        } catch (IOException e) {
            logException("Cannot read " + jvmRunFile, e);
        } catch (ServerDelegateException e) {
            logException("Cannot upload JvmRun data", e);
        }
    }

    private void logException(String msg, Exception e) {
        if (log.isDebugEnabled() && !(getRootCause(e) instanceof ConnectException)) {
            // log with full stack trace
            log.error(msg, e);
        } else {
            // log a one-liner with the root cause
            log.error("{}: {}", msg, getRootCause(e).toString());
        }
    }

    private void analyzeAndUploadCodeBaseIfNeeded(CodeBase newCodeBase) {
        if (jvmRun != null && !newCodeBase.equals(codeBase)) {
            newCodeBase.scanSignatures(codeBaseScanner);
            try {
                serverDelegate.uploadSignatureData(jvmRun.getJvmFingerprint(), newCodeBase.getSignatures());
                codeBase = newCodeBase;
            } catch (ServerDelegateException e) {
                logException("Cannot upload signature data", e);
            }
        }
    }

    private void processUsageDataIfNeeded() {
        if (jvmRun != null && codeBase != null) {
            List<Usage> usages = FileUtils.consumeAllUsageDataFiles(config.getUsageFile());
            if (!usages.isEmpty()) {
                storeNormalizedUsages(usages);
                uploadUsedSignatures();
            }
        }
    }

    private void uploadUsedSignatures() {
        try {
            serverDelegate.uploadUsageData(jvmRun.getJvmFingerprint(), signatureUsage.getNotUploadedSignatures());
            signatureUsage.clearNotUploadedSignatures();
            FileUtils.deleteAllConsumedUsageDataFiles(config.getUsageFile());
        } catch (ServerDelegateException e) {
            logException("Cannot upload usage data", e);
        }
    }

    int storeNormalizedUsages(List<Usage> usages) {
        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Usage usage : usages) {
            String rawSignature = usage.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);

            UsageConfidence confidence = null;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                confidence = UsageConfidence.EXACT_MATCH;
            } else {
                String baseSignature = codeBase.getBaseSignature(normalizedSignature);
                if (baseSignature != null) {
                    log.debug("{} replaced by {}", normalizedSignature, baseSignature);

                    overridden += 1;
                    confidence = UsageConfidence.FOUND_IN_PARENT_CLASS;
                    normalizedSignature = baseSignature;
                } else if (normalizedSignature.equals(rawSignature)) {
                    unrecognized += 1;
                    confidence = UsageConfidence.NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    confidence = UsageConfidence.NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            signatureUsage.put(normalizedSignature, usage.getUsedAtMillis(), confidence);
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
