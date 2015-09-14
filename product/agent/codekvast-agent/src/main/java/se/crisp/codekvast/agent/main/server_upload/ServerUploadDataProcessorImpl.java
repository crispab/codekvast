package se.crisp.codekvast.agent.main.server_upload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.codebase.CodeBase;
import se.crisp.codekvast.agent.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.main.*;
import se.crisp.codekvast.agent.model.Invocation;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.util.ComputerID;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.agent.util.LoggingUtil;
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiException;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * An implementation of DataProcessor that uploads all data to a remote server using the {@link AgentApi}.
 */
@Component
@Slf4j
public class ServerUploadDataProcessorImpl implements DataProcessor {

    private final AgentConfig config;
    private final AgentApi agentApi;
    private final CodeBaseScanner codeBaseScanner;
    private final AppVersionResolver appVersionResolver;
    private final InvocationsCollector invocationsCollector;
    private final TransactionHelper transactionHelper;
    private final String agentComputerId = ComputerID.compute().toString();
    private final String agentHostName = getHostName();

    @Inject
    public ServerUploadDataProcessorImpl(AgentConfig config,
                                         AgentApi agentApi,
                                         CodeBaseScanner codeBaseScanner,
                                         AppVersionResolver appVersionResolver,
                                         InvocationsCollector invocationsCollector,
                                         TransactionHelper transactionHelper) {
        this.config = config;
        this.agentApi = agentApi;
        this.codeBaseScanner = codeBaseScanner;
        this.appVersionResolver = appVersionResolver;
        this.invocationsCollector = invocationsCollector;
        this.transactionHelper = transactionHelper;
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
                LoggingUtil.logException(log, "Cannot upload JVM data to " + agentApi.getServerUri(), e);
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
                LoggingUtil.logException(log, "Cannot upload signature data to " + agentApi.getServerUri(), e);
            }
        }
    }

    @Override
    public void processInvocationsData(long now, JvmState jvmState) {
        List<Invocation> invocations = FileUtils.consumeAllInvocationDataFiles(jvmState.getInvocationsFile());
        if (jvmState.getCodeBase() != null && !invocations.isEmpty()) {
            transactionHelper.storeNormalizedInvocations(jvmState, invocations);
            uploadNotUploadedSignatures(jvmState);
        }
    }


    private JvmData getJvmData(JvmState jvmState) {
        Jvm jvm = jvmState.getJvm();

        return JvmData.builder()
                      .agentComputerId(agentComputerId)
                      .agentHostName(agentHostName)
                      .agentTimeMillis(System.currentTimeMillis())
                      .agentUploadIntervalSeconds(config.getServerUploadIntervalSeconds())
                      .agentVcsId(config.getAgentVcsId())
                      .agentVersion(config.getAgentVersion())
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
            LoggingUtil.logException(log, "Cannot upload invocation data to " + agentApi.getServerUri(), e);
        }
    }


}
