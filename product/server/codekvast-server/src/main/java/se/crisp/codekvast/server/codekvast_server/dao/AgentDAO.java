package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.event.internal.CollectorUptimeEvent;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

/**
 * A data access object for things related to the agent API.
 *
 * @author Olle Hallin
 */
public interface AgentDAO {

    /**
     * Retrieve an application ID. If not found, a new row is inserted into APPLICATIONS and an ApplicationCreatedEvent is posted on the
     * event bus.
     */
    long getAppId(long organisationId, String appName, String appVersion) throws UndefinedApplicationException;

    /**
     * Stores invocation data in the database.
     *
     *
     * @param appId The identity of the application
     * @param invocationData The invocation data to store.
     * @return The data that has actually been inserted in the database (i.e., duplicates are eliminated)
     */
    InvocationData storeInvocationData(AppId appId, InvocationData invocationData);

    /**
     * Stores data about a JVM run
     *
     * @param organisationId The organisation's id
     * @param appId          The application's id
     * @param data           The JVM data received from the collector
     */
    void storeJvmData(long organisationId, long appId, JvmData data);

    /**
     * Create a uptime event for a certain organisation
     *
     * @param organisationId The organisation
     * @return An event to post on the EventBus
     */
    CollectorUptimeEvent createCollectorUpTimeEvent(long organisationId);
}
