package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;

/**
 * An event posted on the internal event bus every time JvmData is received from any agent.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class CollectorDataEvent {
    Collection<CollectorEntry> collectors;
    Collection<String> usernames;

    @Value
    @Builder
    public static class CollectorEntry {
        private final String name;
        private final String version;
        private final int trulyDeadAfterSeconds;
        private final long startedAtMillis;
        private final long dumpedAtMillis;
    }
}
