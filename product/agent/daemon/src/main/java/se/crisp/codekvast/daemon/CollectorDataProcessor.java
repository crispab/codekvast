package se.crisp.codekvast.daemon;

import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;

/**
 * Strategy for how to process data from the Codekvast collector.
 */
public interface CollectorDataProcessor {
    void processCollectorData(JvmState jvmState, CodeBase codeBase) throws DataProcessingException;
}
