package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.agent.model.v1.InvocationData;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.JvmData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import java.util.Collection;

/**
 * The storage API.
 *
 * @author Olle Hallin
 */
public interface StorageService {

    /**
     * Stores JVM run data received from an agent.
     *
     * @param data The received JVM run data
     */
    void storeJvmRunData(JvmData data) throws CodekvastException;

    /**
     * Stores signature data received from an agent.
     *
     * @param data The received signature data
     */
    void storeSignatureData(SignatureData data) throws CodekvastException;

    /**
     * Stores invocation data received from an agent.
     *
     * @param data The received invocation data
     */
    void storeInvocationsData(InvocationData data) throws CodekvastException;

    /**
     * Retrieve all signatures for a certain customer.
     *
     * @param customerName
     * @return A list of invocation entries. Does never return null.
     */
    Collection<InvocationEntry> getSignatures(String customerName) throws CodekvastException;
}
