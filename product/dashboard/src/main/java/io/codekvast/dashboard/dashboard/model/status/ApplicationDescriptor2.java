package io.codekvast.dashboard.dashboard.model.status;

import lombok.*;

/**
 * @author olle.hallin@crisp.se
 */
@Data
@Setter(AccessLevel.NONE)
@Builder
@EqualsAndHashCode(of = {"appName", "environment"})
public class ApplicationDescriptor2 {
    @NonNull
    private final String appName;

    @NonNull
    private final String environment;

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

    // Computed fields to make it usable with Gson, which only serializes fields.
    private int collectedDays;

    public ApplicationDescriptor2 computeFields() {
        int oneDayInMillis = 24 * 60 * 60 * 1000;
        this.collectedDays = Math.toIntExact((collectedToMillis - collectedSinceMillis) / oneDayInMillis);
        return this;
    }
}
