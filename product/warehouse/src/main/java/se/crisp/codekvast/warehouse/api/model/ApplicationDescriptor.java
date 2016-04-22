package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;

/**
 * Data about the application versions a particular method appears in.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class ApplicationDescriptor {
    /**
     * When was this application version first seen?
     */
    @NonNull
    private final Long startedAtMillis;

    /**
     * When was the last time we received data from this application version?
     */
    @NonNull
    private final Long dumpedAtMillis;

    /**
     * When was this particular method invoked in this application version?
     */
    @NonNull
    private final Long invokedAtMillis;

    /**
     * What is the status of this particular method for this application version?
     */
    @NonNull
    private final SignatureStatus status;
}
