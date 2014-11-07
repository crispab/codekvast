package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Value;
import lombok.experimental.Builder;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class User {
    private final String fullName;
    private final String emailAddress;
    private final String username;
}
