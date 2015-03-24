package se.crisp.codekvast.agent.main;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.main.appversion.AppVersionStrategy;
import se.crisp.codekvast.agent.main.codebase.CodeBase;
import se.crisp.codekvast.agent.main.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.model.Invocation;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.util.ComputerID;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiException;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This is the meat of the codekvast-agent. It contains a scheduled method that uploads changed data to the codekvast-server.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class AgentWorker {

    private final AgentConfig config;
    private final CodeBaseScanner codeBaseScanner;
    private final AgentApi agentApi;
    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<>();
    private final String agentComputerId = ComputerID.compute().toString();
    private final String agentHostName = getHostName();

    private final Map<String, JvmState> jvmStates = new HashMap<>();
    private long now;

    @Inject
    public AgentWorker(AgentApi agentApi,
                       AgentConfig config,
                       CodeBaseScanner codeBaseScanner,
                       Collection<AppVersionStrategy> appVersionStrategies) {
        this.agentApi = agentApi;
        this.config = config;
        this.codeBaseScanner = codeBaseScanner;
        this.appVersionStrategies.addAll(appVersionStrategies);

        log.info("{} {} started", getClass().getSimpleName(), config.getDisplayVersion());
    }

    @PreDestroy
    public void shutdownHook() {
        log.info("{} {} shuts down", getClass().getSimpleName(), config.getDisplayVersion());
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Cannot get name of localhost");
            return "-- unknown --";
        }
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.serverUploadIntervalSeconds}000")
    public void analyseCollectorData() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(getClass().getSimpleName());
        try {
            doAnalyzeCollectorData();
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    private void doAnalyzeCollectorData() {
        log.debug("Analyzing collector data");
        now = System.currentTimeMillis();

        findJvmStates();

        for (JvmState jvmState : jvmStates.values()) {
            if (jvmState.getInvocationDataUploadedAt() == 0L) {
                // The agent might have crashed between consuming invocation data files and uploading them to the server.
                // Make sure that invocation data is not lost...
                FileUtils.resetAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());
            }
            uploadJvmData(jvmState);
            analyzeAndUploadCodeBaseIfNeeded(jvmState, new CodeBase(jvmState.getJvm().getCollectorConfig()));
            processInvocationsDataIfNeeded(jvmState);
        }
    }

    private void findJvmStates() {
        findJvmState(config.getDataPath());
    }

    private void findJvmState(File dataPath) {
        log.debug("Looking for jvm.dat in {}", dataPath);

        File[] files = dataPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(CollectorConfig.JVM_BASENAME)) {
                    addOrUpdateJvmState(file);
                } else if (file.isDirectory()) {
                    findJvmState(file);
                }
            }
        }
    }

    private void addOrUpdateJvmState(File file) {
        try {

            Jvm jvm = Jvm.readFrom(file);

            JvmState jvmState = jvmStates.get(jvm.getJvmUuid());
            if (jvmState == null) {
                jvmState = new JvmState();
                jvmStates.put(jvm.getJvmUuid(), jvmState);
            }
            jvmState.setJvm(jvm);
            resolveAppVersion(jvmState);

            jvmState.setInvocationsFile(new File(file.getParentFile(), CollectorConfig.INVOCATIONS_BASENAME));
        } catch (IOException e) {
            log.error("Cannot load " + file, e);
        }
    }

    private void uploadJvmData(JvmState jvmState) {
        Jvm jvm = jvmState.getJvm();

        if (jvmState.getJvmDataUploadedAt() < jvm.getDumpedAtMillis()) {
            try {
                //@formatter:off

                agentApi.uploadJvmData(getJvmData(jvmState));
                //@formatter:on
                jvmState.setJvmDataUploadedAt(jvm.getDumpedAtMillis());
            } catch (AgentApiException e) {
                logException("Cannot upload JVM data to " + agentApi.getServerUri(), e);
            }
        }
    }

    private JvmData getJvmData(JvmState jvmState) {
        Jvm jvm = jvmState.getJvm();

        return JvmData.builder()
                      .agentComputerId(agentComputerId)
                      .agentHostName(agentHostName)
                      .agentUploadIntervalSeconds(config.getServerUploadIntervalSeconds())
                      .appName(jvm.getCollectorConfig().getAppName())
                      .appVersion(jvmState.getAppVersion())
                      .codekvastVcsId(config.getAgentVcsId())
                      .codekvastVersion(config.getAgentVersion())
                      .collectorComputerId(jvm.getComputerId())
                      .collectorHostName(jvm.getHostName())
                      .collectorResolutionSeconds(jvm.getCollectorConfig().getCollectorResolutionSeconds())
                      .dumpedAtMillis(jvm.getDumpedAtMillis())
                      .jvmUuid(jvm.getJvmUuid())
                      .methodVisibility(jvm.getCollectorConfig().getMethodVisibility())
                      .startedAtMillis(jvm.getStartedAtMillis())
                      .tags(jvm.getCollectorConfig().getTags())
                      .build();
    }

    static String resolveAppVersion(Collection<? extends AppVersionStrategy> appVersionStrategies, Collection<File> codeBases, String
            appVersion) {
        String version = appVersion.trim();
        String args[] = version.split("\\s+");

        for (AppVersionStrategy strategy : appVersionStrategies) {
            if (strategy.canHandle(args)) {
                long startedAt = System.currentTimeMillis();
                String resolvedVersion = strategy.resolveAppVersion(codeBases, args);
                log.debug("Resolved '{}' to '{}' in {} ms", version, resolvedVersion, System.currentTimeMillis() - startedAt);
                return resolvedVersion;
            }
        }
        log.debug("Cannot resolve appVersion '{}', using it verbatim", version);
        return version;
    }

    private void logException(String msg, Exception e) {
        Throwable rootCause = getRootCause(e);
        if (log.isDebugEnabled() && !(rootCause instanceof ConnectException)) {
            // log with full stack trace
            log.error(msg, e);
        } else {
            // log a one-liner with the root cause
            log.error("{}: {}", msg, rootCause.toString());
        }
    }

    private void analyzeAndUploadCodeBaseIfNeeded(JvmState jvmState, CodeBase newCodeBase) {
        if (jvmState.getCodebaseUploadedAt() == 0 || !newCodeBase.equals(jvmState.getCodeBase())) {
            if (jvmState.getCodebaseUploadedAt() == 0) {
                log.debug("Codebase has not yet been uploaded");
            } else {
                resolveAppVersion(jvmState);
                log.info("Codebase has changed, it will now be re-scanned and uploaded");
            }

            codeBaseScanner.scanSignatures(newCodeBase);

            try {
                agentApi.uploadSignatureData(getJvmData(jvmState), newCodeBase.getSignatures());
                jvmState.setCodeBase(newCodeBase);
                jvmState.setCodebaseUploadedAt(now);
            } catch (AgentApiException e) {
                logException("Cannot upload signature data to " + agentApi.getServerUri(), e);
            }
        }
    }

    private void resolveAppVersion(JvmState jvmState) {
        String oldAppVersion = jvmState.getAppVersion();

        Jvm jvm = jvmState.getJvm();

        String newAppVersion = resolveAppVersion(appVersionStrategies, jvm.getCollectorConfig().getCodeBaseFiles(),
                                                 jvm.getCollectorConfig().getAppVersion());

        if (oldAppVersion == null) {
            log.info("{} has version '{}'", jvmState.getJvm().getCollectorConfig().getAppName(), newAppVersion);
        } else if (!newAppVersion.equals(oldAppVersion)) {
            log.info("The version of {} has changed from '{}' to '{}'", jvmState.getJvm().getCollectorConfig().getAppName(),
                     oldAppVersion, newAppVersion);
        }

        jvmState.setAppVersion(newAppVersion);
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
            agentApi.uploadInvocationData(getJvmData(jvmState),
                                          Lists.newArrayList(jvmState.getInvocationsCollector().getNotUploadedInvocations()));
            jvmState.getInvocationsCollector().clearNotUploadedSignatures();
            FileUtils.deleteAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());
        } catch (AgentApiException e) {
            logException("Cannot upload invocation data to " + agentApi.getServerUri(), e);
            // Don't reset consumed invocation files, the data is still in the InvocationsCollector
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
                    log.debug("Unrecognized signature: {}", normalizedSignature);
                } else {
                    unrecognized += 1;
                    confidence = SignatureConfidence.NOT_FOUND_IN_CODE_BASE;
                    log.debug("Unrecognized signature: {} (was {})", normalizedSignature, rawSignature);
                }
            }

            if (normalizedSignature != null) {
                jvmState.getInvocationsCollector().put(normalizedSignature,
                                                       invocation.getInvokedAtMillis(),
                                                       invocation.getInvokedAtMillis() - jvmState.getJvm().getStartedAtMillis(),
                                                       confidence);
            }
        }

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored method invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.debug("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
    }

    private Throwable getRootCause(Throwable t) {
        return t.getCause() == null ? t : getRootCause(t.getCause());
    }

    @Data
    private static class JvmState {
        private final InvocationsCollector invocationsCollector = new InvocationsCollector();

        private Jvm jvm;
        private File invocationsFile;
        private CodeBase codeBase;
        private String appVersion;
        private long jvmDataUploadedAt;
        private long codebaseUploadedAt;
        private long invocationDataUploadedAt;
    }
}
