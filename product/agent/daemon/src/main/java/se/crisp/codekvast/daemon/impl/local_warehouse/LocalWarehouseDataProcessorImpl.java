package se.crisp.codekvast.daemon.impl.local_warehouse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.daemon.impl.AbstractDataProcessorImpl;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.shared.model.Invocation;

/**
 * An implementation of DataProcessor that stores invocation data in a local data warehouse.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("localWarehouse")
@Slf4j
public class LocalWarehouseDataProcessorImpl extends AbstractDataProcessorImpl {

    public LocalWarehouseDataProcessorImpl(DaemonConfig config,
                                           AppVersionResolver appVersionResolver,
                                           CodeBaseScanner codeBaseScanner) {
        super(config, appVersionResolver, codeBaseScanner);
        log.info("{} created", getClass().getSimpleName());
    }

    @Override
    protected void doProcessJvmData(JvmState jvmState) {

    }

    @Override
    protected void doProcessCodebase(long now, JvmState jvmState, CodeBase codeBase) {

    }

    @Override
    protected void doStoreNormalizedSignature(JvmState jvmState, Invocation invocation, String normalizedSignature,
                                              SignatureConfidence confidence) {

    }

    @Override
    protected void doProcessUnprocessedSignatures(JvmState jvmState) {

    }

}
