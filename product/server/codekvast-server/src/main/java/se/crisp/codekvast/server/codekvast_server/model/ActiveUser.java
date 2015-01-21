package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Value;
import lombok.experimental.Builder;

import java.util.Date;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class ActiveUser {
    private final String sessionId;
    private final String username;
    private final Date loggedInAt;
}
