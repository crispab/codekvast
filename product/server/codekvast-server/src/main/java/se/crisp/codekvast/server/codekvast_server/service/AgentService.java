package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.agent.model.v1.SensorData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;

/**
 * The service layer interface for the agent REST API.
 *
 * @author Olle Hallin
 */
public interface AgentService {

    /**
     * Stores sensor data received from an agent.
     *
     * @param data The received sensor data
     */
    void storeSensorData(SensorData data);

    /**
     * Stores signature data received from an agent.
     *
     * @param data The received signature data
     */
    void storeSignatureData(SignatureData data);

    /**
     * Stores usage data received from an agent.
     *
     * @param data The received usage data
     */
    void storeUsageData(UsageData data);
}
