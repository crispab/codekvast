package se.crisp.codekvast.server.codekvast_server.model.event.display;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A display object for one application.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ApplicationDisplay {
    @NonNull
    private String name;
    int usageCycleSeconds;
}
