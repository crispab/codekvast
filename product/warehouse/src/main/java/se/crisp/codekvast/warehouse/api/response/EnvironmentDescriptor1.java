package se.crisp.codekvast.warehouse.api.response;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Information the environments a particular method appears in.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@EqualsAndHashCode(of = "name")
public class EnvironmentDescriptor1 implements Comparable<EnvironmentDescriptor1> {

    @NonNull
    private final String name;

    /**
     * In what hosts does the particular method appear?
     */
    @NonNull
    @Singular
    private final Set<String> hostNames;

    /**
     * What tags are configured for Codekvast in the environments for this particular method?
     */
    @NonNull
    @Singular
    private final Set<String> tags;

    /**
     * When was collection started in this environment?
     */
    @NonNull
    private final Long collectedSinceMillis;

    /**
     * When was the last instant collection data was received from this environment?
     */
    @NonNull
    private final Long collectedToMillis;

    /**
     * When was the last instant this particular method was invoked in this environment?
     */
    @NonNull
    private final Long invokedAtMillis;

    /**
     * Convenience: the difference between collectedToMillis and collectedSinceMillis expressed as days.
     */
    @SuppressWarnings("unused")
    public Integer getCollectedDays() {
        int oneDayInMillis = 24 * 60 * 60 * 1000;
        return Math.toIntExact((collectedToMillis - collectedSinceMillis) / oneDayInMillis);
    }

    public EnvironmentDescriptor1 mergeWith(EnvironmentDescriptor1 that) {
        return that == null ? this
                : EnvironmentDescriptor1.builder()
                                        .name(this.name)
                                        .invokedAtMillis(Math.max(this.invokedAtMillis, that.invokedAtMillis))
                                        .collectedToMillis(Math.max(this.collectedToMillis, that.collectedToMillis))
                                        .collectedSinceMillis(Math.min(this.collectedSinceMillis, that.collectedSinceMillis))
                                        .hostNames(add(this.hostNames, that.hostNames))
                                        .tags(add(this.tags, that.tags))
                                        .build();
    }

    private Set<String> add(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.addAll(right);
        return result;
    }

    @Override
    public int compareTo(EnvironmentDescriptor1 that) {
        return this.name.compareTo(that.name);
    }
}
