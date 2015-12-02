package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.Value;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class EnvironmentDisplay {
    String name;

    private Collection<String> hostNames;
}
