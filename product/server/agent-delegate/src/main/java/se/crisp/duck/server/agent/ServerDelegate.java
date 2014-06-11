package se.crisp.duck.server.agent;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * This is the business delegate interface used by a Duck agent for communicating with the server.
 *
 * @author Olle Hallin
 */
public interface ServerDelegate {
    /**
     * Uploads data about a sensor to the server.
     *
     * @param hostName        The host name of the sensor
     * @param startedAtMillis The instant the sensor was started
     * @param dumpedAtMillis  The instant the latest usage dump was made
     * @param uuid            The UUID of the sensor
     * @throws ServerDelegateException
     */
    void uploadSensorData(String hostName, long startedAtMillis, long dumpedAtMillis, UUID uuid) throws ServerDelegateException;

    /**
     * Upload a collection of signatures to the server.
     * <p/>
     * This is typically done when the agent starts and then each time it detects a change in the code base.
     *
     * @param signatures The complete set of signatures found in the application
     * @throws ServerDelegateException Should the upload fail for some reason.
     */
    void uploadSignatureData(Collection<String> signatures) throws ServerDelegateException;

    /**
     * Upload method usage to the server.
     * <p/>
     * This is done as soon as a new usage file is produced by the sensor.
     *
     * @param usage A map with signature as key and the time instant (millis since epoch) the method was invoked as value.
     * @throws ServerDelegateException
     */
    void uploadUsageData(Map<String, Long> usage) throws ServerDelegateException;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Value
    @Builder
    static class Config {

        @NonNull
        private final String customerName;

        @NonNull
        private final String appName;

        @NonNull
        private final String codeBaseName;

        @NonNull
        private final String environment;

        @NonNull
        private final URI serverUri;
    }
}
