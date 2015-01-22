package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

/**
 * Responsible for the agent-facing business logic.
 *
 * @author Olle Hallin
 */
public interface AgentService {

    /**
     * Stores JVM data received from an agent.
     *
     * @param data The received JVM data
     */
    void storeJvmData(JvmData data) throws CodekvastException;

    /**
     * Stores signature data received from an agent.
     *
     * @param data The received signature data
     */
    void storeSignatureData(SignatureData data) throws CodekvastException;

    /**
     * Stores invocation data received from an agent.
     *
     * @param data The received invocation data
     */
    void storeInvocationData(InvocationData data) throws CodekvastException;

}
