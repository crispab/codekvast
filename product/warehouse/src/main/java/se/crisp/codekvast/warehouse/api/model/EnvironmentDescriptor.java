package se.crisp.codekvast.warehouse.api.model;

import lombok.*;

import java.util.Set;

/**
 * Information about collection in a certain environment,
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@EqualsAndHashCode(of = {"name"})
public class EnvironmentDescriptor {
    @NonNull
    private final String name;

    @NonNull
    @Singular
    private final Set<String> hostNames;

    @NonNull
    @Singular
    private final Set<String> tags;

    @NonNull
    private final Long collectedSinceMillis;

    @NonNull
    private final Integer collectedDays;
}
