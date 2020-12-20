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
package io.codekvast.common.customer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Default values for all defined price plans.
 *
 * <p>The names of the plans corresponds 1-to-1 to rows in the table price_plans.
 *
 * <p>The price plans are also defined at Heroku (except for DEMO).
 *
 * @author olle.hallin@crisp.se
 * @see "https://addons.heroku.com/provider/addons/codekvast/plans"
 * @see
 *     "https://docs.google.com/spreadsheets/d/1FJN9YBk0Yxp6Npb0mW3yMQmcYSIrv_Tux3SOnjRTjvE/edit?folder=0B8WxpL3zK856WktGTDM5YUpIMG8#gid=0"
 */
@Getter
@RequiredArgsConstructor
public enum PricePlanDefaults {
  DEMO(25_000, 1, 5, 5, 5, -1, 30),
  TEST(25_000, 3, 3_600, 600, 60, 60, 14),
  BRONZE(50_000, 25, 3_600, 900, 60, -1, 14),
  SILVER(100_000, 250, 7_200, 1_200, 60, -1, 30),
  GOLD(250_000, 1_000, 7_200, 1_800, 60, -1, 60),
  PLATINUM(500_000, 3_000, 7_200, 1_800, 60, -1, 90);

  private final int maxMethods;
  private final int maxNumberOfAgents;
  private final int publishIntervalSeconds;
  private final int pollIntervalSeconds;
  private final int retryIntervalSeconds;
  private final int trialPeriodDays;
  private final int retentionPeriodDays;

  public String toDatabaseName() {
    return name().toLowerCase();
  }

  public static PricePlanDefaults ofDatabaseName(String planName) {
    return PricePlanDefaults.valueOf(planName.toUpperCase());
  }
}
