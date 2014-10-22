package se.crisp.codekvast.server.codekvast_server.service;

import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.SignatureData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;

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
    void storeJvmRunData(JvmRunData data) throws CodekvastException;

    /**
     * Stores signature data received from an agent.
     *
     * @param data The received signature data
     */
    void storeSignatureData(SignatureData data) throws CodekvastException;

    /**
     * Stores usage data received from an agent.
     *
     * @param data The received usage data
     */
    void storeUsageData(UsageData data) throws CodekvastException;

    /**
     * Retrieve all signatures for a certain customer.
     *
     * @param customerName
     * @return A list of usage data entries. Does never return null.
     */
    Collection<UsageDataEntry> getSignatures(String customerName) throws CodekvastException;
}
