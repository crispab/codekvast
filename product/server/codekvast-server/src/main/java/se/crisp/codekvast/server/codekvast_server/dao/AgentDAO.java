package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent.model.v1.InvocationData;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import java.util.Collection;

/**
 * A data access object for things related to the agent API.
 *
 * @author Olle Hallin
 */
public interface AgentDAO {

    void storeJvmData(JvmData jvmData) throws CodekvastException;

    /**
     * Stores invocation data in the database.
     *
     * @param invocationData The invocation data to store.
     * @return The actually stored or updated invocation entries.
     * @throws CodekvastException
     */
    Collection<InvocationEntry> storeInvocationData(InvocationData invocationData) throws CodekvastException;
}
