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
import se.crisp.codekvast.agent.main.codebase.CodeBase;
import se.crisp.codekvast.agent.main.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.main.support.AppVersionStrategy;
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
import java.net.URI;
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
    private final Map<String, String> appVersions = new HashMap<>();
    private final Map<String, CodeBase> codeBases = new HashMap<>();
    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<>();

    @Inject
    public AgentWorker(@Value("${info.build.gradle.version}") String codekvastGradleVersion,
                       @Value("${info.build.git.id}") String codekvastVcsId,
                       ServerDelegate serverDelegate,
                       AgentConfig config,
                       CodeBaseScanner codeBaseScanner,
                       Collection<AppVersionStrategy> appVersionStrategies) {
        Preconditions.checkArgument(!codekvastGradleVersion.contains("{info.build"));
        Preconditions.checkArgument(!codekvastVcsId.contains("{info.build"));
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.serverDelegate = serverDelegate;
        this.codekvastGradleVersion = codekvastGradleVersion;
        this.codekvastVcsId = codekvastVcsId;
        this.appVersionStrategies.addAll(appVersionStrategies);
        log.info("Starting agent worker {} ({})", codekvastGradleVersion, codekvastVcsId);
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.serverUploadIntervalMillis}")
    public void analyseCollectorData() {
        log.debug("Analyzing collector data");
        long now = System.currentTimeMillis();

        for (JvmState jvmState : findJvmStates()) {
            Jvm jvm = jvmState.getJvm();

            String appVersion = getAppVersion(jvm);

            String fingerprint = jvm.getJvmFingerprint();
            Long oldProcessedAt = jvmProcessedAt.get(fingerprint);

            if (oldProcessedAt == null) {
                // The agent might have crashed between consuming invocation data files and uploading them to the server.
                // Make sure that invocation data is not lost...
                FileUtils.resetAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());

                uploadJvmData(jvm, appVersion);

                analyzeAndUploadCodeBaseIfNeeded(jvmState, new CodeBase(jvm.getCollectorConfig()));

                processInvocationsDataIfNeeded(jvmState);
            } else if (oldProcessedAt < jvm.getDumpedAtMillis()) {
                uploadJvmData(jvm, appVersion);

                processInvocationsDataIfNeeded(jvmState);
            }
            jvmProcessedAt.put(fingerprint, now);
        }
    }

    private String getAppVersion(Jvm jvm) {
        String appVersion = appVersions.get(jvm.getJvmFingerprint());
        if (appVersion == null) {
            appVersion = resolveAppVersion(appVersionStrategies, jvm.getCollectorConfig().getNormalizedCodeBaseUris(),
                                           jvm.getCollectorConfig().getAppVersion());
            appVersions.put(jvm.getJvmFingerprint(), appVersion);
        }
        return appVersion;
    }

    private Collection<JvmState> findJvmStates() {
        Collection<JvmState> result = new ArrayList<>();
        findJvmState(result, config.getSharedConfig().getDataPath());
        return result;
    }

    private void findJvmState(Collection<JvmState> result, File dataPath) {
        log.debug("Looking for jvm.dat in {}", dataPath);

        File[] files = dataPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(CollectorConfig.JVM_BASENAME)) {
                    addJvmState(result, file);
                } else if (file.isDirectory()) {
                    findJvmState(result, file);
                }
            }
        }
    }

    private void addJvmState(Collection<JvmState> result, File file) {
        try {
            JvmState jvmState = JvmState.builder()
                                        .invocationsFile(new File(file.getParentFile(), CollectorConfig.INVOCATIONS_BASENAME))
                                        .jvm(Jvm.readFrom(file)).build();
            jvmState.setCodeBase(codeBases.get(jvmState.getJvm().getJvmFingerprint()));
            result.add(jvmState);
        } catch (IOException e) {
            log.error("Cannot load " + file, e);
        }
    }

    private void uploadJvmData(Jvm jvm, String appVersion) {
        try {
            serverDelegate.uploadJvmData(
                    jvm.getCollectorConfig().getCustomerName(),
                    jvm.getCollectorConfig().getAppName(),
                    appVersion,
                    jvm.getHostName(),
                    jvm.getStartedAtMillis(),
                    jvm.getDumpedAtMillis(),
                    jvm.getJvmFingerprint(),
                    codekvastGradleVersion,
                    codekvastVcsId);
        } catch (ServerDelegateException e) {
            logException("Cannot upload JVM data to " + serverDelegate.getServerUri(), e);
        }
    }

    static String resolveAppVersion(Collection<? extends AppVersionStrategy> appVersionStrategies, Collection<URI> codeBaseUris, String
            appVersion) {
        String version = appVersion.trim();
        String args[] = version.split("\\s+");

        for (AppVersionStrategy strategy : appVersionStrategies) {
            if (strategy.canHandle(args)) {
                long startedAt = System.currentTimeMillis();
                String resolvedVersion = strategy.resolveAppVersion(codeBaseUris, args);
                log.debug("Resolved '{}' to '{}' in {} ms", version, resolvedVersion, System.currentTimeMillis() - startedAt);
                return resolvedVersion;
            }
        }
        log.warn("Cannot resolve appVersion '{}', using it verbatim", version);
        return version;
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
            codeBaseScanner.scanSignatures(newCodeBase);
            try {
                serverDelegate.uploadSignatureData(jvmState.getJvm().getJvmFingerprint(), newCodeBase.getSignatures());
                jvmState.setCodeBase(newCodeBase);
                codeBases.put(jvmState.getJvm().getJvmFingerprint(), newCodeBase);
            } catch (ServerDelegateException e) {
                logException("Cannot upload signature data to " + serverDelegate.getServerUri(), e);
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

    void storeNormalizedInvocations(JvmState jvmState, List<Invocation> invocations) {
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

            jvmState.getInvocationsCollector().put(normalizedSignature, invocation.getInvokedAtMillis(), confidence);
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored signature invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.info("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
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
