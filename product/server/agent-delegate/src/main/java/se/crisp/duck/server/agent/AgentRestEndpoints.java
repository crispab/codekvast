package se.crisp.duck.server.agent;

/**
 * @author Olle Hallin
 */
public class AgentRestEndpoints {

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.SensorData} object to this endpoint.
     */
    public static final String UPLOAD_SENSOR_V1 = "/agent/sensor/v1";

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.SignatureData} object to this endpoint.
     */
    public static final String UPLOAD_SIGNATURES_V1 = "/agent/signatures/v1";

    /**
     * POST a {@link se.crisp.duck.server.agent.model.v1.UsageData} object to this endpoint.
     */
    public static final String UPLOAD_USAGE_V1 = "/agent/usage/v1";
}
