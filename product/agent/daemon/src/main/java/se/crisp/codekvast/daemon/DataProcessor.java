package se.crisp.codekvast.daemon;

import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.impl.DataProcessingException;

/**
 * Strategy for how to process data.
 */
public interface DataProcessor {
    void processData(long now, JvmState jvmState, CodeBase codeBase) throws DataProcessingException;
}
