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
package io.codekvast.dashboard.customer;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

/**
 * Data that describes a customer.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class CustomerData {
    @NonNull
    private Long customerId;

    @NonNull
    private String customerName;

    @NonNull
    private String source;

    @NonNull
    private PricePlan pricePlan;

    private Instant collectionStartedAt;

    private Instant trialPeriodEndsAt;

    public boolean isTrialPeriodExpired(Instant now) {
        return trialPeriodEndsAt != null && trialPeriodEndsAt.isBefore(now);
    }
}
