package se.crisp.codekvast.server.agent;

import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;

import java.util.Collection;

/**
 * This is the business delegate interface used by a Codekvast agent for communicating with the server.
 *
 * @author Olle Hallin
 */
public interface ServerDelegate {
    /**
     * Uploads data about a JVM run to the server.
     *
     * @param appName          The name of the instrumented application which the JVM runs.
     * @param appVersion       The version of the instrumented app.
     * @param hostName         The host name of the JVM
     * @param startedAtMillis  The instant the JVM was started
     * @param dumpedAtMillis   The instant the latest invocations dump was made
     * @param jvmFingerprint   The unique id of the JVM run
     * @param codekvastVersion Which version of codekvast produced this data?
     * @param codekvastVcsId   The Git hash of the code that produced this data.
     * @throws ServerDelegateException
     */
    void uploadJvmRunData(String appName, String appVersion, String hostName, long startedAtMillis, long dumpedAtMillis,
                          String jvmFingerprint, String codekvastVersion, String codekvastVcsId) throws ServerDelegateException;

    /**
     * Upload a collection of signatures to the server.
     * <p/>
     * This should typically be done when the agent detects that a JVM has started and then each time it detects a change in the code base.
     *
     * @param jvmFingerprint The unique id of the JVM run
     * @param signatures     The complete set of signatures found in the application
     * @throws ServerDelegateException Should the upload fail for some reason.
     */
    void uploadSignatureData(String jvmFingerprint, Collection<String> signatures) throws ServerDelegateException;

    /**
     * Upload method invocations to the server.
     * <p/>
     * This should be done as soon as a new invocations file is produced by the collector.
     *
     * @param jvmFingerprint The fingerprint of the JVM that produced this invocations data.
     * @param invocations    A collection of invocations entries.
     * @throws ServerDelegateException
     */
    void uploadInvocationsData(String jvmFingerprint, Collection<InvocationEntry> invocations) throws ServerDelegateException;

    /**
     * Pings the server.
     *
     * @param message An arbitrary message
     * @return A decorated version of the message
     */
    String ping(String message) throws ServerDelegateException;
}
