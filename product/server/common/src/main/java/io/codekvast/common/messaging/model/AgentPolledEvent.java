/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.common.messaging.model;

import java.time.Instant;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * An event that is sent when an agent polls after the expiration of the trial period.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
public class AgentPolledEvent implements CodekvastEvent {
  @NonNull Long customerId;
  @NonNull String appName;
  @NonNull String environment;
  @NonNull String jvmUuid;
  @NonNull Instant polledAt;
  @NonNull Boolean disabledEnvironment;
  @NonNull Boolean afterTrialPeriod;
  @NonNull Boolean tooManyLiveAgents;
  Instant trialPeriodEndsAt;

  public static AgentPolledEvent sample() {
    return AgentPolledEvent.builder()
        .afterTrialPeriod(false)
        .appName("appName")
        .customerId(1L)
        .disabledEnvironment(false)
        .environment("environment")
        .jvmUuid("jvmUuid")
        .polledAt(Instant.now())
        .tooManyLiveAgents(false)
        .trialPeriodEndsAt(null)
        .build();
  }

  public boolean isAgentEnabled() {
    return !disabledEnvironment && !afterTrialPeriod && !tooManyLiveAgents;
  }
}
