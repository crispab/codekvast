package se.crisp.codekvast.daemon.impl.local_warehouse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.DataProcessor;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;

/**
 * An implementation of DataProcessor that stores invocation data in a local data warehouse.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile("localWarehouse")
@Slf4j
public class LocalWarehouseDataProcessorImpl implements DataProcessor {

    public LocalWarehouseDataProcessorImpl() {
        log.info("{} created", getClass().getSimpleName());
    }

    @Override
    public void processJvmData(long now, JvmState jvmState) {

    }

    @Override
    public void processCodeBase(long now, JvmState jvmState, CodeBase codeBase) {

    }

    @Override
    public void processInvocationsData(long now, JvmState jvmState) {

    }
}
