package se.crisp.codekvast.agent.daemon.worker.http_post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.beans.JvmState;
import se.crisp.codekvast.agent.daemon.codebase.CodeBase;
import se.crisp.codekvast.agent.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.daemon.model.v1.JvmData;
import se.crisp.codekvast.agent.daemon.model.v1.SignatureConfidence;
import se.crisp.codekvast.agent.daemon.worker.AbstractCollectorDataProcessorImpl;
import se.crisp.codekvast.agent.daemon.worker.DataProcessingException;
import se.crisp.codekvast.server.daemon_api.DaemonApi;
import se.crisp.codekvast.server.daemon_api.DaemonApiException;

import javax.inject.Inject;

import static se.crisp.codekvast.agent.daemon.DaemonConstants.HTTP_POST_PROFILE;

/**
 * An implementation of CollectorDataProcessor that uploads all collected data to a remote server using the HTTP POST API that is embedded
 * in {@link DaemonApi}.
 */
@Component
@Profile(HTTP_POST_PROFILE)
@Slf4j
public class HttpPostCollectorDataProcessorImpl extends AbstractCollectorDataProcessorImpl {

    private final DaemonApi daemonApi;
    private final InvocationsCollector invocationsCollector;

    @Inject
    public HttpPostCollectorDataProcessorImpl(DaemonConfig config,
                                              AppVersionResolver appVersionResolver,
                                              CodeBaseScanner codeBaseScanner,
                                              DaemonApi daemonApi,
                                              InvocationsCollector invocationsCollector) {
        super(config, appVersionResolver, codeBaseScanner);
        this.daemonApi = daemonApi;
        this.invocationsCollector = invocationsCollector;
        log.info("{} created", getClass().getSimpleName());
    }

    @Override
    protected void doProcessJvmData(JvmState jvmState) throws DataProcessingException {
        try {
            daemonApi.uploadJvmData(createUploadJvmData(createJvmData(jvmState)));
        } catch (DaemonApiException e) {
            throw new DataProcessingException("Cannot upload JVM data to " + daemonApi.getServerUri(), e);
        }
    }

    private se.crisp.codekvast.server.daemon_api.model.v1.JvmData createUploadJvmData(JvmData jvmData) {
        //@formatter:off
        return se.crisp.codekvast.server.daemon_api.model.v1.JvmData
                .builder()
                .appName(jvmData.getAppName())
                .appVersion(jvmData.getAppVersion())
                .collectorComputerId(jvmData.getCollectorComputerId())
                .collectorHostName(jvmData.getCollectorHostName())
                .collectorResolutionSeconds(jvmData.getCollectorResolutionSeconds())
                .collectorVcsId(jvmData.getCollectorVcsId())
                .collectorVersion(jvmData.getCollectorVersion())
                .daemonComputerId(jvmData.getDaemonComputerId())
                .daemonHostName(jvmData.getDaemonHostName())
                .daemonTimeMillis(System.currentTimeMillis())
                .daemonUploadIntervalSeconds(jvmData.getDataProcessingIntervalSeconds())
                .daemonVcsId(jvmData.getDaemonVcsId())
                .daemonVersion(jvmData.getDaemonVersion())
                .dumpedAtMillis(jvmData.getDumpedAtMillis())
                .jvmUuid(jvmData.getJvmUuid())
                .methodVisibility(jvmData.getMethodVisibility())
                .startedAtMillis(jvmData.getStartedAtMillis())
                .tags(jvmData.getTags())
                .build();
        //@formatter:on
    }

    @Override
    protected void doProcessCodebase(JvmState jvmState, CodeBase codeBase) throws DataProcessingException {
        try {
            daemonApi.uploadSignatureData(createUploadJvmData(createJvmData(jvmState)), codeBase.getSignatures().keySet());
        } catch (DaemonApiException e) {
            throw new DataProcessingException("Cannot upload signature data to " + daemonApi.getServerUri(), e);
        }
    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, String normalizedSignature, long invokedAtMillis,
                                              SignatureConfidence confidence) {
        invocationsCollector
                .put(jvmState.getJvm().getJvmUuid(), jvmState.getJvm().getStartedAtMillis(), normalizedSignature, invokedAtMillis,
                     confidence);

    }

    @Override
    protected void doProcessUnprocessedInvocations(JvmState jvmState) throws DataProcessingException {
        try {
            daemonApi.uploadInvocationData(createUploadJvmData(createJvmData(jvmState)),
                                           invocationsCollector.getNotUploadedInvocations(jvmState.getJvm().getJvmUuid()));
            invocationsCollector.clearNotUploadedSignatures(jvmState.getJvm().getJvmUuid());
        } catch (DaemonApiException e) {
            throw new DataProcessingException("Cannot upload invocation data to " + daemonApi.getServerUri(), e);
        }
    }
}
