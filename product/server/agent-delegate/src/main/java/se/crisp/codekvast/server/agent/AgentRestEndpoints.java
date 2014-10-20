package se.crisp.codekvast.server.agent;

/**
 * Defines the various REST endpoints the server exposes.
 *
 * @author Olle Hallin
 */
public interface AgentRestEndpoints {

    /**
     * POST a {@link se.crisp.codekvast.server.agent.model.test.Ping} object to this endpoint. You will get a {@link
     * se.crisp.codekvast.server.agent.model.test.Pong} in response.
     */
    final String PING = "/agent/test/ping";

    /**
     * POST a {@link se.crisp.codekvast.server.agent.model.v1.JvmRunData} object to this endpoint.
     */
    final String UPLOAD_JVM_RUN_V1 = "/agent/jvm-run/v1";

    /**
     * POST a {@link se.crisp.codekvast.server.agent.model.v1.SignatureData} object to this endpoint.
     */
    final String UPLOAD_SIGNATURES_V1 = "/agent/signatures/v1";

    /**
     * POST a {@link se.crisp.codekvast.server.agent.model.v1.UsageData} object to this endpoint.
     */
    final String UPLOAD_USAGE_V1 = "/agent/usage/v1";
}
