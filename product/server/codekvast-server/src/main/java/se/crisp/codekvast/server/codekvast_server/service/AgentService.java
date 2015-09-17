package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

/**
 * Responsible for the agent-facing business logic.
 *
 * @author olle.hallin@crisp.se
 */
public interface AgentService {

    /**
     * Stores JVM data received from an agent.
     *
     * @param data The received JVM data
     */
    void storeJvmData(String agentApiID, JvmData data) throws CodekvastException;

    /**
     * Stores invocation data received from an agent.
     *
     * @param data The received invocation data
     */
    void storeSignatureData(SignatureData data) throws CodekvastException;

}
