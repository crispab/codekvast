/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.warehouse.webapp.model.status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * Data about the agent status for a certain customer.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class GetStatusResponse1 {
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

    /**
     * How many days of collection does the price plan permit?
     * -1 if no limit.
     */
    @NonNull
    private final Integer maxCollectionPeriodDays;

    //--- actual values ---------------------------------------------
    /**
     * How many methods does this customer have?
     */
    @NonNull
    private final Integer numMethods;

    /**
     * Since when have this customer been using Codekvast?
     */
    @NonNull
    private final Long collectedSinceMillis;

    /**
     * How long has this customer been using Codekvast?
     */
    @NonNull
    private final Integer collectedDays;

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
     * The agents (JVMs) reporting to this customer account.
     */
    private final List<AgentDescriptor1> agents;

    /**
     * The interactive users that have logged in to this customer.
     */
    private final List<UserDescriptor1> users;
}
