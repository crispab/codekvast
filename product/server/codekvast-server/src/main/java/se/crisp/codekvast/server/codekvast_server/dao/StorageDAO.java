package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;

import java.util.Collection;

/**
 * @author Olle Hallin
 */
public interface StorageDAO {
    void storeJvmRunData(JvmRunData jvmRunData) throws CodekvastException;

    /**
     * Stores usage data in the database.
     *
     * @param usageData The usage data to store.
     * @return The actually stored or updated usage data entries.
     * @throws CodekvastException
     */
    Collection<UsageDataEntry> storeUsageData(UsageData usageData) throws CodekvastException;

    Collection<UsageDataEntry> getSignatures(String customerName) throws CodekvastException;
}
