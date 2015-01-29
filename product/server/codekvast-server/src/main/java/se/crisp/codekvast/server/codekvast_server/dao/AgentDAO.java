package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

/**
 * A data access object for things related to the agent API.
 *
 * @author Olle Hallin
 */
public interface AgentDAO {

    /**
     * Stores invocation data in the database.
     *
     *
     * @param appId The identity of the application
     * @param invocationData The invocation data to store.
     */
    void storeInvocationData(AppId appId, InvocationData invocationData);

    /**
     * Stores data about a JVM run
     *
     * @param organisationId The organisation's id
     * @param appId          The application's id
     * @param data           The JVM data received from the collector
     */
    void storeJvmData(long organisationId, long appId, JvmData data);

    CollectorTimestamp getCollectorTimestamp(long organisationId);
}
