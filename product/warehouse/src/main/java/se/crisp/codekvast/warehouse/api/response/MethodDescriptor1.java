package se.crisp.codekvast.warehouse.api.response;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import se.crisp.codekvast.warehouse.api.DescribeSignature1Parameters;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class MethodDescriptor1 {
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
    private final SortedSet<ApplicationDescriptor1> occursInApplications;

    @Singular
    private final SortedSet<EnvironmentDescriptor1> collectedInEnvironments;

    /**
     * Maximum value of occursInApplications.invokedAtMillis;
     */
    public Long getLastInvokedAtMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getInvokedAtMillis).reduce(Math::max).orElse(0L);
    }

    /**
     * Minimum value of occursInApplications.startedAtMillis
     */
    public Long getCollectedSinceMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getStartedAtMillis).reduce(Math::min).orElse(0L);
    }

    /**
     * Maximum value of occursInApplications.getDumpedAtMillis
     */
    public Long getCollectedToMillis() {
        return occursInApplications.stream().map(ApplicationDescriptor1::getDumpedAtMillis).reduce(Math::max).orElse(0L);
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
        collectedInEnvironments.stream().map(EnvironmentDescriptor1::getTags).forEach(tags -> result.addAll(tags));
        return result;
    }

    public static Comparator<MethodDescriptor1> getComparator(DescribeSignature1Parameters.OrderBy orderBy) {
        switch (orderBy) {
        case INVOKED_AT_ASC:
            return new ByInvokedAtComparatorAsc();
        case INVOKED_AT_DESC:
            return new ByInvokedAtComparatorDesc();
        case SIGNATURE:
            return new BySignatureComparator();
        }
        throw new IllegalArgumentException("Unknown OrderBy: " + orderBy);
    }

    public static class ByInvokedAtComparatorAsc implements Comparator<MethodDescriptor1> {
        @Override
        public int compare(MethodDescriptor1 o1, MethodDescriptor1 o2) {
            return o1.getLastInvokedAtMillis().compareTo(o2.getLastInvokedAtMillis());
        }
    }

    public static class ByInvokedAtComparatorDesc implements Comparator<MethodDescriptor1> {
        @Override
        public int compare(MethodDescriptor1 o1, MethodDescriptor1 o2) {
            return o2.getLastInvokedAtMillis().compareTo(o1.getLastInvokedAtMillis());
        }
    }

    public static class BySignatureComparator implements Comparator<MethodDescriptor1> {
        @Override
        public int compare(MethodDescriptor1 o1, MethodDescriptor1 o2) {
            return o1.getSignature().compareTo(o2.getSignature());
        }
    }
}
