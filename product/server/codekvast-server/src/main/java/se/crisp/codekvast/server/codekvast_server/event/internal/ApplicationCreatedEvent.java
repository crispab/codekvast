package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.Application;

import java.util.Collection;

/**
 * An event posted when a new row is inserted in the APPLICATIONS table.
 *
 * @author Olle Hallin
 */
@Value
public class ApplicationCreatedEvent {
    private final Application application;
    private final Collection<String> usernames;
}
