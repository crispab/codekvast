package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;

/**
 * Responsible for the daemon-facing business logic.
 *
 * @author olle.hallin@crisp.se
 */
public interface DaemonService {

    /**
     * Stores JVM data received from a daemon.
     *
     * @param apiAccessID The identity of the daemon that is accessing the API.
     * @param data The received JVM data
     */
    void storeJvmData(String apiAccessID, JvmData data) throws CodekvastException;

    /**
     * Stores invocation data received from a daemon.
     *
     * @param data The received invocation data
     */
    void storeSignatureData(SignatureData data);

}
