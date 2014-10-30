package se.crisp.codekvast.agent.main;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.config.AgentConfig;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.model.Usage;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.v1.UsageConfidence;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

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
    private final String codekvastGradleVersion;
    private final String codekvastVcsId;
    private final Map<String, Long> jvmProcessedAt = new HashMap<>();

    @Inject
    public AgentWorker(@Value("${info.build.gradle.version}") String codekvastGradleVersion,
                       @Value("${info.build.git.id}") String codekvastVcsId, ServerDelegate serverDelegate, AgentConfig config,
                       CodeBaseScanner codeBaseScanner) {
        Preconditions.checkArgument(!codekvastGradleVersion.contains("{info.build"));
        Preconditions.checkArgument(!codekvastVcsId.contains("{info.build"));
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
        this.codekvastGradleVersion = codekvastGradleVersion;
        this.codekvastVcsId = codekvastVcsId;
        log.debug("Starting agent worker {} ({})", codekvastGradleVersion, codekvastVcsId);
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.serverUploadIntervalMillis}")
    public void analyseCollectorData() {
        log.debug("Analyzing collector data");
        long now = System.currentTimeMillis();

        for (JvmState jvmState : findJvmStates()) {
            Jvm jvm = jvmState.getJvm();

            String fingerprint = jvm.getJvmFingerprint();
            Long oldProcessedAt = jvmProcessedAt.get(fingerprint);

            if (oldProcessedAt == null) {
                // The agent might have crashed between consuming usage data files and uploading them to the server.
                // Make sure that usage data is not lost...
                FileUtils.resetAllConsumedUsageDataFiles(jvmState.getUsageFile());

                uploadJvmRun(jvm);

                analyzeAndUploadCodeBaseIfNeeded(jvmState, new CodeBase(config, jvm.getCollectorConfig().getCodeBaseUri(), jvm
                        .getCollectorConfig().getAppName()));

                processUsageDataIfNeeded(jvmState);
            } else if (oldProcessedAt < jvm.getDumpedAtMillis()) {
                uploadJvmRun(jvm);

                processUsageDataIfNeeded(jvmState);
            }
            jvmProcessedAt.put(fingerprint, now);
        }
    }

    private Collection<JvmState> findJvmStates() {
        Collection<JvmState> result = new ArrayList<>();
        findJvmRuns(result, config.getSharedConfig().getDataPath());
        return result;
    }

    private void findJvmRuns(Collection<JvmState> result, File dataPath) {
        log.debug("Looking for jvm-run.dat in {}", dataPath);

        File[] files = dataPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(CollectorConfig.JVM_RUN_BASENAME)) {
                    addJvmRun(result, file);
                } else if (file.isDirectory()) {
                    findJvmRuns(result, file);
                }
            }
        }
    }

    private void addJvmRun(Collection<JvmState> result, File file) {
        try {
            result.add(JvmState.builder()
                               .usageFile(new File(file.getParentFile(), CollectorConfig.USAGE_BASENAME))
                               .jvm(Jvm.readFrom(file)).build());
        } catch (IOException e) {
            log.error("Cannot load " + file, e);
        }
    }

    private void uploadJvmRun(Jvm jvm) {
        try {
            serverDelegate.uploadJvmRunData(
                    jvm.getCollectorConfig().getAppName(),
                    jvm.getCollectorConfig().getAppVersion(),
                    jvm.getHostName(),
                    jvm.getStartedAtMillis(),
                    jvm.getDumpedAtMillis(),
                    jvm.getJvmFingerprint(),
                    codekvastGradleVersion,
                    codekvastVcsId);
        } catch (ServerDelegateException e) {
            logException("Cannot upload JVM data", e);
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

    private void analyzeAndUploadCodeBaseIfNeeded(JvmState jvmState, CodeBase newCodeBase) {
        if (!newCodeBase.equals(jvmState.getCodeBase())) {
            newCodeBase.scanSignatures(codeBaseScanner);
            try {
                serverDelegate.uploadSignatureData(jvmState.getJvm().getJvmFingerprint(), newCodeBase.getSignatures());
                jvmState.setCodeBase(newCodeBase);
            } catch (ServerDelegateException e) {
                logException("Cannot upload signature data", e);
            }
        }
    }

    private void processUsageDataIfNeeded(JvmState jvmState) {
        List<Usage> usages = FileUtils.consumeAllUsageDataFiles(jvmState.getUsageFile());
        if (jvmState.getCodeBase() != null && !usages.isEmpty()) {
            storeNormalizedUsages(jvmState, usages);
            uploadUsedSignatures(jvmState);
        }
    }

    private void uploadUsedSignatures(JvmState jvmState) {
        try {
            serverDelegate
                    .uploadUsageData(jvmState.getJvm().getJvmFingerprint(), jvmState.getSignatureUsage().getNotUploadedSignatures());
            jvmState.getSignatureUsage().clearNotUploadedSignatures();
            FileUtils.deleteAllConsumedUsageDataFiles(jvmState.getUsageFile());
        } catch (ServerDelegateException e) {
            logException("Cannot upload usage data", e);
        }
    }

    int storeNormalizedUsages(JvmState jvmState, List<Usage> usages) {
        CodeBase codeBase = jvmState.getCodeBase();

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

            jvmState.getSignatureUsage().put(normalizedSignature, usage.getUsedAtMillis(), confidence);
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

    @Data
    @Builder
    private static class JvmState {
        private final Jvm jvm;
        private final File usageFile;
        private final SignatureUsage signatureUsage = new SignatureUsage();
        private CodeBase codeBase;
    }
}
