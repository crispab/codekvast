package se.crisp.duck.server.agent;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.net.URI;
import java.util.Collection;

/**
 * This is the business delegate interface used by a Duck agent for communicating with the server.
 *
 * @author Olle Hallin
 */
public interface ServerDelegate {
    /**
     * Upload a collection of signatures to the server.
     * <p/>
     * This is typically done when the agent starts and then each time it detects a change in the code base.
     *
     * @param signatures The complete set of signatures found in the application
     * @throws ServerDelegateException Should the upload fail for some reason.
     */
    void uploadSignatures(Collection<String> signatures) throws ServerDelegateException;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Value
    @Builder
    static class Config {

        @NonNull
        private final String customerName;

        @NonNull
        private final String appName;

        @NonNull
        private final String environment;

        @NonNull
        private final URI serverUri;
    }
}
