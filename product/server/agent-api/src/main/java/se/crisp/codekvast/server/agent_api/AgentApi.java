package se.crisp.codekvast.server.agent_api;

import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;

import java.net.URI;
import java.util.Collection;

/**
 * This is the business delegate interface used by a Codekvast agent for communicating with the server.
 *
 * @author Olle Hallin
 */
public interface AgentApi {
    /**
     * Uploads data about a JVM run to the server.
     *
     * @throws AgentApiException For all problems.
     */
    void uploadJvmData(JvmData jvmData) throws AgentApiException;

    /**
     * Upload a collection of signatures to the server.
     *
     * This should typically be done when the agent detects that a JVM has started and then each time it detects a change in the code base.
     *
     * @param jvmFingerprint The unique id of the JVM run
     * @param signatures     The complete set of signatures found in the application
     * @throws AgentApiException Should the upload fail for some reason.
     */
    void uploadSignatureData(String jvmFingerprint, Collection<String> signatures) throws AgentApiException;

    /**
     * Upload method invocations to the server.
     *
     * This should be done as soon as a new invocations file is produced by the collector.
     *
     * @param jvmFingerprint The fingerprint of the JVM that produced this invocations data.
     * @param invocations    A collection of invocations entries.
     * @throws AgentApiException For all problems.
     */
    void uploadInvocationsData(String jvmFingerprint, Collection<InvocationEntry> invocations) throws AgentApiException;

    /**
     * Pings the server.
     *
     * @param message An arbitrary message.
     * @return A decorated version of the message.
     * @throws AgentApiException For all problems.
     */
    String ping(String message) throws AgentApiException;

    /**
     * To which server are we connected? Useful when logging problems.
     *
     * @return The URI we use for connecting to the server.
     */
    URI getServerUri();
}
