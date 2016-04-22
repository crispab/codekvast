package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class MethodDescriptor {
    @NonNull
    private final Long id;

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
    private final String modifiers;

    private final String packageName;

    private final String declaringType;

    /**
     * Copied over from codekvast-collector.conf
     */
    @Singular
    private final SortedSet<String> tags;

    @Singular
    private final SortedMap<ApplicationId, ApplicationDescriptor> occursInApplications;

    @Singular
    private final SortedMap<String, EnvironmentDescriptor> collectedInEnvironments;

    /**
     * Maximum value of occursInApplications.invokedAtMillis;
     */
    public long getLastInvokedAtMillis() {
        return occursInApplications.values().stream().map(ApplicationDescriptor::getInvokedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Minimum value of occursInApplications.startedAtMillis
     */
    public long getCollectedSinceMillis() {
        return occursInApplications.values().stream().map(ApplicationDescriptor::getStartedAtMillis).reduce(Math::min).orElse(0L);
    }

    /**
     * Maximum value of occursInApplications.getDumpedAtMillis
     */
    public long getCollectedToMillis() {
        return occursInApplications.values().stream().map(ApplicationDescriptor::getDumpedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Convenience: the difference between {@link #getCollectedToMillis()} and {@link #getCollectedSinceMillis()} expressed as days.
     */
    @SuppressWarnings("unused")
    public int getCollectedDays() {
        int dayInMillis = 24 * 60 * 60 * 1000;
        return Math.toIntExact((getCollectedToMillis() - getCollectedSinceMillis()) / dayInMillis);
    }

}
