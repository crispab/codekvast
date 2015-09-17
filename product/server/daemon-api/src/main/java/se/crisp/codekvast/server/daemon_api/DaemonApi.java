package se.crisp.codekvast.server.daemon_api;

import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureEntry;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * This is the business delegate interface used by a Codekvast daemon for communicating with the server.
 *
 * @author olle.hallin@crisp.se
 */
public interface DaemonApi {
    /**
     * Uploads data about a JVM run to the server.
     *
     * @param jvmData Data about the JVM
     * @throws DaemonApiException For all problems.
     */
    void uploadJvmData(JvmData jvmData) throws DaemonApiException;

    /**
     * Upload a collection of signatures to the server.
     *
     * This should typically be done when the daemon detects that a JVM has started and then each time it detects a change in the code base.
     *
     * @param jvmData Data about the JVM
     * @param signatures     The complete set of signatures found in the application
     * @throws DaemonApiException Should the upload fail for some reason.
     */
    void uploadSignatureData(JvmData jvmData, Collection<String> signatures) throws DaemonApiException;

    /**
     * Upload data about method invocations to the server.
     *
     * This should be done as soon as a new invocations file is produced by the collector.
     *
     * @param jvmData Data about the JVM
     * @param invocations    A collection of invocations entries.
     * @throws DaemonApiException For all problems.
     */
    void uploadInvocationData(JvmData jvmData, List<SignatureEntry> invocations) throws DaemonApiException;

    /**
     * Pings the server.
     *
     * @param message An arbitrary message.
     * @return A decorated version of the message.
     * @throws DaemonApiException For all problems.
     */
    String ping(String message) throws DaemonApiException;

    /**
     * To which server are we connected? Useful when logging problems.
     *
     * @return The URI we use for connecting to the server.
     */
    URI getServerUri();
}
