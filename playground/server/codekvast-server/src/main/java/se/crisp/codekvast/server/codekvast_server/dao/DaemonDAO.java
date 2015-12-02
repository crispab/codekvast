package se.crisp.codekvast.server.codekvast_server.dao;

import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.model.AppId;
import se.crisp.codekvast.server.codekvast_server.model.event.display.WebSocketMessage;
import se.crisp.codekvast.server.codekvast_server.model.event.rest.OrganisationSettings;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureData;

import java.util.Collection;

/**
 * A data access object for things related to the daemon API.
 *
 * @author olle.hallin@crisp.se
 */
public interface DaemonDAO {

    /**
     * Retrieve an application ID. If not found, a new row is inserted into APPLICATIONS and an ApplicationCreatedEvent is posted on the
     * event bus.
     */
    long getAppId(long organisationId, String appName) throws UndefinedApplicationException;

    /**
     * Retrieve an application ID by JVM id.
     */
    AppId getAppIdByJvmUuid(String jvmUuid);

    /**
     * Convert the given application names to a list of AppIds
     *
     * @return Does never return null
     */
    Collection<AppId> getApplicationIds(long organisationId, Collection<String> applicationNames);

    /**
     * Retrieve all application IDs by name/version/hostname
     *
     * @return Does never return null
     */
    Collection<AppId> getApplicationIds(long organisationId, String appName, String appVersion, String hostname);

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
     * @param organisationId    The organisation
     * @param organisationSettings The new settings
     *
     * @return A collection of application names that were actually updated. Does never return null.
     */
    Collection<String> saveSettings(long organisationId, OrganisationSettings organisationSettings);

    /**
     * Synchronously recalculate the statistics for a certain app.
     *
     * @param appId The identity of the application
     */
    void recalculateApplicationStatistics(AppId appId);

    /**
     * Delete all signature and jvm_info rows for a certain app/version/hostname.
     *
     * @return The number of database rows that were deleted.
     */
    int deleteCollectors(long organisationId, String appName, String appVersion, String hostName);

    /**
     * Retrieve the number of collectors for a certain application name.
     */
    int getNumCollectors(long organisationId, String appName);

    /**
     * Retrieve the number of collectors for a certain application name/version.
     */
    int getNumCollectors(long organisationId, String appName, String appVersion);

    /**
     * Delete the application row and it's statistics.
     *
     * @return The number of database rows that were deleted.
     */
    int deleteApplication(long organisationId, String appName);

    /**
     * Delete statistics for a certain app/version
     *
     * @return The number of database rows that were deleted.
     */
    int deleteApplicationStatistics(long organisationId, String appName, String appVersion);
}
