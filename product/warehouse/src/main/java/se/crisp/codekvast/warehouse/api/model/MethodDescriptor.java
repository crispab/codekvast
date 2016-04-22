package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

    @Singular
    private final SortedSet<ApplicationDescriptor> occursInApplications;

    @Singular
    private final SortedSet<EnvironmentDescriptor> collectedInEnvironments;

    /**
     * Maximum value of occursInApplications.invokedAtMillis;
     */
    public long getLastInvokedAtMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor::getInvokedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Minimum value of occursInApplications.startedAtMillis
     */
    public long getCollectedSinceMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor::getStartedAtMillis).reduce(Math::min).orElse(0L);
    }

    /**
     * Maximum value of occursInApplications.getDumpedAtMillis
     */
    public long getCollectedToMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor::getDumpedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Convenience: the difference between {@link #getCollectedToMillis()} and {@link #getCollectedSinceMillis()} expressed as days.
     */
    @SuppressWarnings("unused")
    public int getCollectedDays() {
        int dayInMillis = 24 * 60 * 60 * 1000;
        return Math.toIntExact((getCollectedToMillis() - getCollectedSinceMillis()) / dayInMillis);
    }

    /**
     * Convenience: collects tags from all environments
     */
    @SuppressWarnings("unused")
    public Set<String> getTags() {
        Set<String> result = new TreeSet<>();
        collectedInEnvironments.stream().map(EnvironmentDescriptor::getTags).forEach(tags -> result.addAll(tags));
        return result;
    }
}
