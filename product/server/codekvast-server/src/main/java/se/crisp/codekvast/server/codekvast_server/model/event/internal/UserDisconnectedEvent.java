package se.crisp.codekvast.server.codekvast_server.model.event.internal;

import lombok.Value;

/**
 * /** An event broadcast on EventBus when a user session has been terminated.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class UserDisconnectedEvent {
    String username;
}
