/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard.model.status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Data about the agent status for a certain customer.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
public class GetStatusResponse {
    //--- Query performance stuff -----------------------------------
    /**
     * When was the request received? Millis since epoch.
     */
    @NonNull
    private final Long timestamp;

    /**
     * How long did it take to execute the request?
     */
    @NonNull
    private final Long queryTimeMillis;

    //--- Price plan ------------------------------------------------
    /**
     * Which price plan does this customer use?
     */
    @NonNull
    private final String pricePlan;

    /**
     * Which retention period does the price plan specify?
     */
    @NonNull
    private final Integer retentionPeriodDays;

    /**
     * Which collection resolution does the price plan specify?
     */
    @NonNull
    private final Integer collectionResolutionSeconds;

    /**
     * How many agents does the price plan allow?
     */
    @NonNull
    private final Integer maxNumberOfAgents;

    /**
     * How many methods does the price plan allow?
     */
    @NonNull
    private final Integer maxNumberOfMethods;

    //--- actual values ---------------------------------------------
    /**
     * At which instant will the trial period end? Null if not in a trial period.
     */
    private final Long trialPeriodEndsAtMillis;

    /**
     * What percentage of the trial period has been used? Null if not in a trial period.
     */
    private final Integer trialPeriodPercent;

    /**
     * Is there a trial period, and if so, has it expired?
     */
    @NonNull
    private final Boolean trialPeriodExpired;

    /**
     * How many methods does this customer have?
     */
    @NonNull
    private final Integer numMethods;

    /**
     * At which instant was the first data received?
     * Null if no data yet has been collected.
     */
    private final Long collectedSinceMillis;

    /**
     * How many agents has ever attempted to deliver data to Codekvast?
     */
    @NonNull
    private final Integer numAgents;

    /**
     * How many live agents are attempting to deliver data to Codekvast?
     */
    @NonNull
    private final Integer numLiveAgents;

    /**
     * How many live and enabled agents are attempting to deliver data to Codekvast?
     */
    @NonNull
    private final Integer numLiveEnabledAgents;

    //--- Details ---------------------------------------------------

    /**
     * The environments this customer has.
     */
    private final List<EnvironmentStatusDescriptor> environments;

    /**
     * The applications reporting to this customer account.
     */
    private final List<ApplicationDescriptor2> applications;

    /**
     * The agents (JVMs) reporting to this customer account.
     */
    private final List<AgentDescriptor> agents;
}
