package se.crisp.codekvast.server.codekvast_server.event.internal;

import lombok.Value;
import se.crisp.codekvast.server.codekvast_server.model.Application;

import java.util.Collection;

/**
 * An event posted when a new row is inserted in the APPLICATIONS table.
 *
 * @author olle.hallin@crisp.se
 */
@Value
public class ApplicationCreatedEvent {
    private final Application application;
    private final String appVersion;
    private final Collection<String> usernames;
}
