package se.crisp.duck.server.agent;

/**
 * @author Olle Hallin
 */
public class AgentRestEndpoints {

    /**
     * POST an {@link se.crisp.duck.server.agent.model.v1.SignatureData} object to this endpoint.
     */
    public static final String UPLOAD_SIGNATURES_V1 = "/agent/signatures/v1";

    /**
     * POST an {@link se.crisp.duck.server.agent.model.v1.UsageData} object to this endpoint.
     */
    public static final String UPLOAD_USAGE_V1 = "/agent/usage/v1";
}
