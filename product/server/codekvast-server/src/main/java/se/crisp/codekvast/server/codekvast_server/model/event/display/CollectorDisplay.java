package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;

/**
 * A display object for one collector.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class CollectorDisplay {
    String name;
    String version;
    String hostname;
    int trulyDeadAfterSeconds;
    long startedAtMillis;
    long dataReceivedAtMillis;
}
