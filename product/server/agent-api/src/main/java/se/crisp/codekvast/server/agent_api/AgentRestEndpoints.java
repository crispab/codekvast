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
    final String UPLOAD_V1_JVM_DATA = "/agent/v1/jvm-data";

    /**
     * POST a {@link se.crisp.codekvast.server.agent_api.model.v1.SignatureData} object to this endpoint.
     */
    final String UPLOAD_V1_SIGNATURES = "/agent/v1/signatures";
}
