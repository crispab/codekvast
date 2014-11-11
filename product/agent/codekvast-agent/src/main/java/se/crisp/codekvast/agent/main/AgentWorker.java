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
import se.crisp.codekvast.agent.model.Invocation;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.model.v1.SignatureConfidence;

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
                // The agent might have crashed between consuming invocation data files and uploading them to the server.
                // Make sure that invocation data is not lost...
                FileUtils.resetAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());

                uploadJvmRun(jvm);

                analyzeAndUploadCodeBaseIfNeeded(jvmState, new CodeBase(config, jvm.getCollectorConfig().getCodeBaseUri(), jvm
                        .getCollectorConfig().getAppName()));

                processInvocationsDataIfNeeded(jvmState);
            } else if (oldProcessedAt < jvm.getDumpedAtMillis()) {
                uploadJvmRun(jvm);

                processInvocationsDataIfNeeded(jvmState);
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
                if (file.isFile() && file.getName().equals(CollectorConfig.JVM_BASENAME)) {
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
                               .invocationsFile(new File(file.getParentFile(), CollectorConfig.INVOCATIONS_BASENAME))
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

    private void processInvocationsDataIfNeeded(JvmState jvmState) {
        List<Invocation> invocations = FileUtils.consumeAllInvocationDataFiles(jvmState.getInvocationsFile());
        if (jvmState.getCodeBase() != null && !invocations.isEmpty()) {
            storeNormalizedInvocations(jvmState, invocations);
            uploadUsedSignatures(jvmState);
        }
    }

    private void uploadUsedSignatures(JvmState jvmState) {
        try {
            serverDelegate
                    .uploadInvocationsData(jvmState.getJvm().getJvmFingerprint(),
                                           jvmState.getInvocationsCollector().getNotUploadedInvocations());
            jvmState.getInvocationsCollector().clearNotUploadedSignatures();
            FileUtils.deleteAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());
        } catch (ServerDelegateException e) {
            logException("Cannot upload invocation data", e);
        }
    }

    int storeNormalizedInvocations(JvmState jvmState, List<Invocation> invocations) {
        CodeBase codeBase = jvmState.getCodeBase();

        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Invocation invocation : invocations) {
            String rawSignature = invocation.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);

            SignatureConfidence confidence = null;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                confidence = SignatureConfidence.EXACT_MATCH;
            } else {
                String baseSignature = codeBase.getBaseSignature(normalizedSignature);
                if (baseSignature != null) {
                    log.debug("{} replaced by {}", normalizedSignature, baseSignature);

                    overridden += 1;
                    confidence = SignatureConfidence.FOUND_IN_PARENT_CLASS;
                    normalizedSignature = baseSignature;
                } else if (normalizedSignature.equals(rawSignature)) {
                    unrecognized += 1;
                    confidence = SignatureConfidence.NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    confidence = SignatureConfidence.NOT_FOUND_IN_CODE_BASE;
                    log.warn("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            jvmState.getInvocationsCollector().put(normalizedSignature, invocation.getUsedAtMillis(), confidence);
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored signature invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.info("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
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
        private final File invocationsFile;
        private final InvocationsCollector invocationsCollector = new InvocationsCollector();
        private CodeBase codeBase;
    }
}
