package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A display object for one collector.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class CollectorDisplay {
    /**
     * The name of the app that this collector is attached to.
     */
    @NonNull
    String name;

    /**
     * The version of the app that this collector is attached to.
     */
    @NonNull
    String version;

    /**
     * The host in which the collected app executes.
     */
    @NonNull
    String hostname;

    /**
     * The length of the usage cycle for the app. After this many seconds, code that has not been used could be considered truly dead.
     */
    int usageCycleSeconds;

    /**
     * When did this collector start?
     * @see System#currentTimeMillis()
     */
    long startedAtMillis;

    /**
     * When did this collector deliver data to the Codekvast server?
     *
     * @see System#currentTimeMillis()
     */
    long dataReceivedAtMillis;
}
