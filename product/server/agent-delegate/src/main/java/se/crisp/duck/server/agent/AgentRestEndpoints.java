package se.crisp.duck.server.agent;

/**
 * Defines the various REST endpoints the server exposes.
 *
 * @author Olle Hallin
 */
public interface AgentRestEndpoints {

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.SensorData} object to this endpoint.
     */
    final String UPLOAD_SENSOR_V1 = "/agent/sensor/v1";

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.SignatureData} object to this endpoint.
     */
    final String UPLOAD_SIGNATURES_V1 = "/agent/signatures/v1";

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.UsageData} object to this endpoint.
     */
    final String UPLOAD_USAGE_V1 = "/agent/usage/v1";
}
