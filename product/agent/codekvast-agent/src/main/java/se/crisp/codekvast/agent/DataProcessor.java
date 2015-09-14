package se.crisp.codekvast.agent;

import se.crisp.codekvast.agent.beans.JvmState;
import se.crisp.codekvast.agent.codebase.CodeBase;

/**
 * Strategy for how to process data.
 */
public interface DataProcessor {
    void processJvmData(long now, JvmState jvmState);

    void processCodeBase(long now, JvmState jvmState, CodeBase codeBase);

    void processInvocationsData(long now, JvmState jvmState);
}
