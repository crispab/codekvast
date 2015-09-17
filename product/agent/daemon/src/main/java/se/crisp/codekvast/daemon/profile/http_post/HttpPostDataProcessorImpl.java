package se.crisp.codekvast.daemon.profile.http_post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.daemon.DataProcessor;
import se.crisp.codekvast.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.daemon.beans.AgentConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.daemon.util.LogUtil;
import se.crisp.codekvast.server.daemon_api.AgentApi;
import se.crisp.codekvast.server.daemon_api.AgentApiException;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.shared.model.Invocation;
import se.crisp.codekvast.shared.model.Jvm;
import se.crisp.codekvast.shared.util.ComputerID;
import se.crisp.codekvast.shared.util.FileUtils;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * An implementation of DataProcessor that uploads all data to a remote server using the HTTP POST API that is embedded in {@link
 * AgentApi}.
 */
@Component
@Profile("httpPost")
@Slf4j
public class HttpPostDataProcessorImpl implements DataProcessor {

    private final AgentConfig config;
    private final AgentApi agentApi;
    private final CodeBaseScanner codeBaseScanner;
    private final AppVersionResolver appVersionResolver;
    private final InvocationsCollector invocationsCollector;
    private final String daemonComputerId = ComputerID.compute().toString();
    private final String daemonHostName = getHostName();

    @Inject
    public HttpPostDataProcessorImpl(AgentConfig config,
                                     AgentApi agentApi,
                                     CodeBaseScanner codeBaseScanner,
                                     AppVersionResolver appVersionResolver,
                                     InvocationsCollector invocationsCollector) {
        this.config = config;
        this.agentApi = agentApi;
        this.codeBaseScanner = codeBaseScanner;
        this.appVersionResolver = appVersionResolver;
        this.invocationsCollector = invocationsCollector;
    }

    @Override
    public void processJvmData(long now, JvmState jvmState) {
        appVersionResolver.resolveAppVersion(jvmState);
        Jvm jvm = jvmState.getJvm();

        if (jvmState.getJvmDataUploadedAt() < jvm.getDumpedAtMillis()) {
            try {
                agentApi.uploadJvmData(getJvmData(jvmState));
                jvmState.setJvmDataUploadedAt(jvm.getDumpedAtMillis());
            } catch (AgentApiException e) {
                LogUtil.logException(log, "Cannot upload JVM data to " + agentApi.getServerUri(), e);
            }
        }
    }

    @Override
    public void processCodeBase(long now, JvmState jvmState, CodeBase codeBase) {
        if (jvmState.getCodebaseUploadedAt() == 0 || !codeBase.equals(jvmState.getCodeBase())) {
            if (jvmState.getCodebaseUploadedAt() == 0) {
                log.debug("Codebase has not yet been uploaded");
            } else {
                appVersionResolver.resolveAppVersion(jvmState);
                log.info("Codebase has changed, it will now be re-scanned and uploaded");
            }

            codeBaseScanner.scanSignatures(codeBase);
            jvmState.setCodeBase(codeBase);

            try {
                agentApi.uploadSignatureData(getJvmData(jvmState), codeBase.getSignatures());
                jvmState.setCodebaseUploadedAt(now);
            } catch (AgentApiException e) {
                LogUtil.logException(log, "Cannot upload signature data to " + agentApi.getServerUri(), e);
            }
        }
    }

    @Override
    @Transactional
    public void processInvocationsData(long now, JvmState jvmState) {
        List<Invocation> invocations = FileUtils.consumeAllInvocationDataFiles(jvmState.getInvocationsFile());
        if (jvmState.getCodeBase() != null && !invocations.isEmpty()) {
            storeNormalizedInvocations(jvmState, invocations);
            uploadNotUploadedSignatures(jvmState);
        }
    }

    private void storeNormalizedInvocations(JvmState jvmState, List<Invocation> invocations) {
        CodeBase codeBase = jvmState.getCodeBase();

        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Invocation invocation : invocations) {
            String rawSignature = invocation.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);
            String baseSignature = codeBase.getBaseSignature(normalizedSignature);

            SignatureConfidence confidence = null;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                confidence = SignatureConfidence.EXACT_MATCH;
            } else if (baseSignature != null) {
                overridden += 1;
                confidence = SignatureConfidence.FOUND_IN_PARENT_CLASS;
                log.debug("{} replaced by {}", normalizedSignature, baseSignature);
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

            storeNormalizedSignature(jvmState, invocation, normalizedSignature, confidence);
        }

        FileUtils.deleteAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());

        // For debugging...
        codeBase.writeSignaturesToDisk();

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored method invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.debug("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
    }

    private void storeNormalizedSignature(JvmState jvmState, Invocation invocation, String normalizedSignature,
                                          SignatureConfidence confidence) {
        if (normalizedSignature != null) {
            invocationsCollector.put(jvmState.getJvm().getJvmUuid(),
                                     jvmState.getJvm().getStartedAtMillis(),
                                     normalizedSignature,
                                     invocation.getInvokedAtMillis(),
                                     confidence);
        }
    }

    private JvmData getJvmData(JvmState jvmState) {
        Jvm jvm = jvmState.getJvm();

        return JvmData.builder()
                      .daemonComputerId(daemonComputerId)
                      .daemonHostName(daemonHostName)
                      .daemonTimeMillis(System.currentTimeMillis())
                      .daemonUploadIntervalSeconds(config.getServerUploadIntervalSeconds())
                      .daemonVcsId(config.getDaemonVcsId())
                      .daemonVersion(config.getDaemonVersion())
                      .appName(jvm.getCollectorConfig().getAppName())
                      .appVersion(jvmState.getAppVersion())
                      .collectorComputerId(jvm.getComputerId())
                      .collectorHostName(jvm.getHostName())
                      .collectorResolutionSeconds(jvm.getCollectorConfig().getCollectorResolutionSeconds())
                      .collectorVcsId(jvm.getCollectorVcsId())
                      .collectorVersion(jvm.getCollectorVersion())
                      .dumpedAtMillis(jvm.getDumpedAtMillis())
                      .jvmUuid(jvm.getJvmUuid())
                      .methodVisibility(jvm.getCollectorConfig().getMethodVisibility().toString())
                      .startedAtMillis(jvm.getStartedAtMillis())
                      .tags(jvm.getCollectorConfig().getTags())
                      .build();
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Cannot get name of localhost");
            return "-- unknown --";
        }
    }

    private void uploadNotUploadedSignatures(JvmState jvmState) {
        try {
            agentApi.uploadInvocationData(getJvmData(jvmState),
                                          invocationsCollector.getNotUploadedInvocations(jvmState.getJvm().getJvmUuid()));
            invocationsCollector.clearNotUploadedSignatures(jvmState.getJvm().getJvmUuid());
        } catch (AgentApiException e) {
            LogUtil.logException(log, "Cannot upload invocation data to " + agentApi.getServerUri(), e);
        }
    }


}
