package se.crisp.codekvast.server.codekvast_server.event;

import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.Application;

/**
 * An event posted when a new row is inserted in the APPLICATIONS table.
 *
 * @author Olle Hallin
 */
@Value
public class ApplicationCreatedEvent {
    private final Application application;
}
