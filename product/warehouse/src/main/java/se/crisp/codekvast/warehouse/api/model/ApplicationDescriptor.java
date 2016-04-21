package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@EqualsAndHashCode(of = {"name", "version"})
public class ApplicationDescriptor {
    @NonNull
    private final String name;

    @NonNull
    private final String version;

    @NonNull
    private final Long invokedAtMillis;

    @NonNull
    private final SignatureStatus status;
}
