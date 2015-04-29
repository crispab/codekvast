package se.crisp.codekvast.server.agent_api;

/**
 * Defines the various REST endpoints the server exposes.
 *
 * @author olle.hallin@crisp.se
 */
public interface AgentRestEndpoints {

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.test.Ping} object to this endpoint. You will get a {@link
     * se.crisp.codekvast.server.agent_api.model.test.Pong} in response.
     */
    final String PING = "/api/agent/ping";

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.v1.JvmData} object to this endpoint.
     */
    final String UPLOAD_V1_JVM_DATA = "/api/agent/v1/jvm-data";

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.v1.SignatureData} object to this endpoint.
     */
    final String UPLOAD_V1_SIGNATURES = "/api/agent/v1/signatures";
}
