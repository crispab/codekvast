package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;

/**
 * An event posted on the internal event bus every time collector data is received from any agent. It contains one display object for each
 * collector and a collection of usernames that are affected by this event.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class CollectorStatusMessage {
    Collection<String> usernames;
    Collection<CollectorDisplay> collectors;
}
