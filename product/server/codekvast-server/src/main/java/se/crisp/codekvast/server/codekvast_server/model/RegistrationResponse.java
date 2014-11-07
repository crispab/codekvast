package se.crisp.codekvast.server.codekvast_server.model;

import lombok.Value;
import lombok.experimental.Builder;

/**
 * @author Olle Hallin
 */
@Value
@Builder
public class RegistrationResponse {
    private String greeting;
}
