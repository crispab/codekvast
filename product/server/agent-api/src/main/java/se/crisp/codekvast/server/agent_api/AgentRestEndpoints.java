package se.crisp.codekvast.server.agent_api;

/**
 * Defines the various REST endpoints the server exposes.
 *
 * @author Olle Hallin
 */
public interface AgentRestEndpoints {

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.test.Ping} object to this endpoint. You will get a {@link
     * se.crisp.codekvast.server.agent_api.model.test.Pong} in response.
     */
    final String PING = "/agent/test/ping";

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.v1.JvmData} object to this endpoint.
     */
    final String UPLOAD_V1_JVM_RUN = "/agent/v1/jvm-run";

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.v1.InvocationData} object to this endpoint.
     */
    final String UPLOAD_V1_INVOCATIONS = "/agent/v1/invocations";
}
