package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.dao.CollectorTimestamp;

import java.util.Collection;

/**
 * An event posted on the internal event bus everytime JvmData is received from any agent
 *
 * @author Olle Hallin
 */
@Value
public class CollectorUptimeEvent {
    CollectorTimestamp collectorTimestamp;
    Collection<String> usernames;
}
