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
package io.codekvast.warehouse.customer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Default values for all defined price plans. Corresponds 1-to-1 to rows in the table price_plans.
 *
 * The price plans are also defined at Heroku (except for those marked as internal).
 *
 * @author olle.hallin@crisp.se
 * @see "https://addons.heroku.com/provider/addons/codekvast/plans"
 */
@Getter
@RequiredArgsConstructor
public enum PricePlanDefaults {
    DEMO(25_000, 1, 5, 5, 5, -1),
    TEST(2_500, 3, 7200, 600, 60, 30);

    private final int maxMethods;
    private final int maxNumberOfAgents;
    private final int publishIntervalSeconds;
    private final int pollIntervalSeconds;
    private final int retryIntervalSeconds;
    private final int maxCollectionPeriodDays;

    public static PricePlanDefaults fromDatabaseName(String planName) {
        return PricePlanDefaults.valueOf(planName.toUpperCase());
    }
}
