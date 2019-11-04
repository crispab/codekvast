package io.codekvast.common.messaging.model;

import io.codekvast.common.customer.PricePlanDefaults;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An event that is sent when a customer violates the license.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class LicenseViolationEvent implements CodekvastEvent {
    @NonNull Long customerId;
    @NonNull String plan;
    @NonNull Integer defaultMaxMethods;
    @NonNull Integer effectiveMaxMethods;
    @NonNull Integer attemptedMethods;

    public static LicenseViolationEvent sample() {
        return LicenseViolationEvent.builder()
                                    .customerId(1L)
                                    .plan(PricePlanDefaults.TEST.name())
                                    .attemptedMethods(4711)
                                    .defaultMaxMethods(100)
                                    .effectiveMaxMethods(1000)
                                    .build();
    }
}
