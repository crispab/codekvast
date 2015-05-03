package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureData;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;

/**
 * A data access object for things related to the agent API.
 *
 * @author olle.hallin@crisp.se
 */
public interface AgentDAO {

    /**
     * Retrieve an application ID. If not found, a new row is inserted into APPLICATIONS and an ApplicationCreatedEvent is posted on the
     * event bus.
     */
    long getAppId(long organisationId, String appName, String appVersion) throws UndefinedApplicationException;

    /**
     * Retrieve an application ID by JVM id.
     */
    AppId getAppIdByJvmUuid(String jvmUuid);

    /**
     * Stores invocation data in the database.
     *
     *
     * @param appId The identity of the application
     * @param signatureData The invocation data to store.
     */
    void storeInvocationData(AppId appId, SignatureData signatureData);

    /**
     * Stores data about a JVM run
     *
     * @param organisationId The organisation's id
     * @param appId          The application's id
     * @param data           The JVM data received from the collector
     */
    void storeJvmData(long organisationId, long appId, JvmData data);

    /**
     * Create a web socket message for a certain organisation
     *
     * @param organisationId The organisation
     * @return An event to post on the EventBus
     */
    WebSocketMessage createWebSocketMessage(long organisationId);

    /**
     * Save updated collector settings
     *
     * @param organisationId    The organisation
     * @param organisationSettings The new settings
     */
    void saveSettings(long organisationId, OrganisationSettings organisationSettings);

    /**
     * Recalculate the statistics for a certain app.
     *
     * @param appId The identity of the application
     */
    void recalculateApplicationStatistics(AppId appId);

    /**
     * Recalculate the statistics for all apps in an organisation..
     *
     * @param organisationId The identity of the organisation
     */
    void recalculateApplicationStatistics(long organisationId);
}
