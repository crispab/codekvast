package se.crisp.duck.server.duck_server.db;

import se.crisp.duck.server.agent.model.v1.SignatureData;

/**
 * @author Olle Hallin
 */
public interface AgentService {

    void storeSignatureData(SignatureData data);
}
