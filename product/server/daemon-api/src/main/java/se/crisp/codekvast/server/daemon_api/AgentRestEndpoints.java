package se.crisp.codekvast.server.daemon_api;

/**
 * Defines the various REST endpoints the server exposes.
 *
 * @author olle.hallin@crisp.se
 */
public interface AgentRestEndpoints {

    String PREFIX = "/api/daemon/";

    /**
     * POST a {@link se.crisp.codekvast.server.daemon_api.model.test.Ping} object to this endpoint. You will get a {@link
     * se.crisp.codekvast.server.daemon_api.model.test.Pong} in response.
     */
    String PING = PREFIX + "ping";

    /**
     * POST a {@link se.crisp.codekvast.server.daemon_api.model.v1.JvmData} object to this endpoint.
     */
    String UPLOAD_V1_JVM_DATA = PREFIX + "v1/jvm-data";

    /**
     * POST a {@link se.crisp.codekvast.server.daemon_api.model.v1.SignatureData} object to this endpoint.
     */
    String UPLOAD_V1_SIGNATURES = PREFIX + "v1/signatures";
}
