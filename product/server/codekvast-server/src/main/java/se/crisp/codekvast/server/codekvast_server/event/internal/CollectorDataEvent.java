package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import lombok.experimental.Builder;

import java.util.Collection;

/**
 * An event posted on the internal event bus every time JvmData is received from any agent.
 *
 * @author Olle Hallin
 */
@Value
public class CollectorDataEvent {
    Collection<CollectorEntry> collectors;
    Collection<String> usernames;

    @Value
    @Builder
    public static class CollectorEntry {
        private final String name;
        private final long startedAtMillis;
        private final long dumpedAtMillis;
    }
}
