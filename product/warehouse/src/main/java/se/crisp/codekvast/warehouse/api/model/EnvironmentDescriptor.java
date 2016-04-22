package se.crisp.codekvast.warehouse.api.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

/**
 * Information the environments a particular method appears in.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class EnvironmentDescriptor {

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
    public Integer getCollectedDays() {
        int oneDayInMillis = 24 * 60 * 60 * 1000;
        return Math.toIntExact((collectedToMillis - collectedSinceMillis) / oneDayInMillis);
    }
}
