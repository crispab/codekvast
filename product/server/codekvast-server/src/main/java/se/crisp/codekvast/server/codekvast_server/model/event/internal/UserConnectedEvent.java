package se.crisp.codekvast.server.codekvast_server.model.event.internal;

import lombok.Value;

/**
 * An event broadcast on EventBus when a user has logged in.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class UserConnectedEvent {
    String username;
}
