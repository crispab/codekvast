package se.crisp.codekvast.daemon;

import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;

/**
 * Strategy for how to process data.
 */
public interface DataProcessor {
    void processJvmData(long now, JvmState jvmState);

    void processCodeBase(long now, JvmState jvmState, CodeBase codeBase);

    void processInvocationsData(long now, JvmState jvmState);
}
