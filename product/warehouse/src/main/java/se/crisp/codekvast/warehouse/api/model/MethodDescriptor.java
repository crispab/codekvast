package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class MethodDescriptor {
    @NonNull
    private final String signature;

    /**
     * public, protected, package-private or private
     */
    @NonNull
    private final String visibility;

    /**
     * static, final, etc
     */
    @NonNull
    private final String modifiers;

    @NonNull
    private final String packageName;

    @NonNull
    private final String declaringType;

    /**
     * Copied over from codekvast-collector.conf
     */
    @Singular
    private final Set<String> tags;

    @Singular
    private final Set<ApplicationDescriptor> occursInApplications;

    @Singular
    private final Set<EnvironmentDescriptor> collectedInEnvironments;

    /**
     * Convenience: maximum value of occursInApplications.invokedAtMillis;
     */
    private final Long lastInvokedAtMillis;

    /**
     * Convenience: the minimum value of collectedInEnvironments.collectedSince
     */
    @NonNull
    private final Long collectedSinceMillis;

    /**
     * Convenience: the maximum value of collectedInEnvironments.collectedDays
     */
    @NonNull
    private final Integer collectedDays;

}
