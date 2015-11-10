package se.crisp.codekvast.daemon.worker.http_post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.daemon.worker.AbstractCollectorDataProcessorImpl;
import se.crisp.codekvast.daemon.worker.DataProcessingException;
import se.crisp.codekvast.server.daemon_api.DaemonApi;
import se.crisp.codekvast.server.daemon_api.DaemonApiException;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence;

import javax.inject.Inject;

import static se.crisp.codekvast.daemon.DaemonConstants.HTTP_POST_PROFILE;

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
            daemonApi.uploadJvmData(createUploadJvmData(jvmState));
        } catch (DaemonApiException e) {
            throw new DataProcessingException("Cannot upload JVM data to " + daemonApi.getServerUri(), e);
        }
    }

    @Override
    protected void doProcessCodebase(JvmState jvmState, CodeBase codeBase) throws DataProcessingException {
        try {
            daemonApi.uploadSignatureData(createUploadJvmData(jvmState), codeBase.getSignatures().keySet());
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
            daemonApi.uploadInvocationData(createUploadJvmData(jvmState),
                                           invocationsCollector.getNotUploadedInvocations(jvmState.getJvm().getJvmUuid()));
            invocationsCollector.clearNotUploadedSignatures(jvmState.getJvm().getJvmUuid());
        } catch (DaemonApiException e) {
            throw new DataProcessingException("Cannot upload invocation data to " + daemonApi.getServerUri(), e);
        }
    }
}
