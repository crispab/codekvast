/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.customer;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Clock;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * The resolved price plan. Contains values from the table price_plan_overrides.
 *
 * Missing row or null column values is defaulted by picking values from {@link PricePlanDefaults}.
 *
 * The price plans are also defined at Heroku (except for those marked as internal).
 *
 * @author olle.hallin@crisp.se
 * @see "https://addons.heroku.com/provider/addons/codekvast/plans"
 */
@Value
@Builder(toBuilder = true)
public class PricePlan {

    @NonNull
    private final String name;

    // These fields will be null unless there is a row in price_plan_overrides
    private final String overrideBy;
    private final String note;

    // These are the effective values to use
    private final int maxMethods;
    private final int maxNumberOfAgents;
    private final int pollIntervalSeconds;
    private final int publishIntervalSeconds;
    private final int retentionPeriodDays;
    private final int retryIntervalSeconds;
    private final int trialPeriodDays;

    /**
     * Adjusts the number of collected days with respect to the retention period.
     * If the retention period is defined (i.e., positive) then return the minimum value of the parameter and the retention period.
     *
     * @param realCollectedDays The real value of collectedDays. May be null.
     * @return The value to present to the user (or null).
     */
    public Integer adjustCollectedDays(Integer realCollectedDays) {
        if (realCollectedDays == null || retentionPeriodDays < 0) {
            return realCollectedDays;
        }
        return Math.min(realCollectedDays, retentionPeriodDays);
    }

    /**
     * Adjusts an instant with respect to the retention period.
     *
     * If a retention period is defined, the the returned instant will not be
     * before the start of the retention period.
     *
     * @param realInstant The real instant (or null).
     * @param clock       The clock to use when calculating beginning of the retention period.
     * @return The value to present to the user.
     */
    public Instant adjustInstant(Instant realInstant, Clock clock) {
        if (realInstant == null || retentionPeriodDays < 0) {
            return realInstant;
        }

        Instant retentionPeriodStart = clock.instant().minus(retentionPeriodDays, DAYS);
        return realInstant.isBefore(retentionPeriodStart) ? retentionPeriodStart : realInstant;
    }

    /**
     * Adjusts an instant with respect to the retention period.
     *
     * If a retention period is defined, the the returned instant will not be
     * before the start of the retention period.
     *
     * @param realInstant The real instant. May be null.
     * @param clock       The clock to use when calculating beginning of the retention period.
     * @return The value to present to the user (or null).
     */
    public Long adjustInstantToMillis(Instant realInstant, Clock clock) {
        Instant instant = adjustInstant(realInstant, clock);
        return instant == null ? null : instant.toEpochMilli();
    }

    /**
     * Adjusts an instant with respect to the retention period.
     *
     * If a retention period is defined, the returned instant will not be
     * before the start of the retention period.
     *
     * @param realTimestampMillis The real instant. May be null.
     * @param clock               The clock to use when calculating beginning of the retention period.
     * @return The value to present to the user (or null).
     */
    public Long adjustTimestampMillis(Long realTimestampMillis, Clock clock) {
        if (realTimestampMillis == null || realTimestampMillis == 0L || retentionPeriodDays <= 0) {
            return realTimestampMillis;
        }
        long retentionPeriodStart = clock.instant().minus(retentionPeriodDays, DAYS).toEpochMilli();
        return realTimestampMillis < retentionPeriodStart ? retentionPeriodStart : realTimestampMillis;
    }

    public static PricePlan of(PricePlanDefaults ppd) {
        return PricePlan.builder()
                        .maxMethods(ppd.getMaxMethods())
                        .maxNumberOfAgents(ppd.getMaxNumberOfAgents())
                        .name(ppd.name())
                        .note(null)
                        .overrideBy(null)
                        .pollIntervalSeconds(ppd.getPollIntervalSeconds())
                        .publishIntervalSeconds(ppd.getPublishIntervalSeconds())
                        .retentionPeriodDays(ppd.getRetentionPeriodDays())
                        .retryIntervalSeconds(ppd.getRetryIntervalSeconds())
                        .trialPeriodDays(ppd.getTrialPeriodDays())
                        .build();
    }
}
