package se.crisp.duck.server.duck_server.service;

import se.crisp.duck.server.agent.model.v1.SignatureData;

/**
 * @author Olle Hallin
 */
public interface AgentService {

    /**
     * Stores signature data received from an agent in the database.
     *
     * @param data The received signature data
     */
    void storeSignatureData(SignatureData data);
}
