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
package io.codekvast.common.lock;

import java.io.File;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

/** @author olle.hallin@crisp.se */
@Value
@Builder
@EqualsAndHashCode(exclude = "connection")
public class Lock {
  @NonNull String name;
  @NonNull String tag;
  Long customerId;
  @NonNull Integer maxLockWaitSeconds;
  @NonNull Integer maxExpectedDurationSeconds;
  Instant waitStartedAt = Instant.now();
  @With Instant acquiredAt;
  @With Connection connection;

  public static Lock forTask(@NonNull String name, int maxExpectedDurationSeconds) {
    return Lock.builder()
        .name(name)
        .tag(name)
        .maxLockWaitSeconds(0)
        .maxExpectedDurationSeconds(maxExpectedDurationSeconds)
        .build();
  }

  public static Lock forCustomer(@NonNull Long customerId) {
    return Lock.builder()
        .name("customer")
        .tag("customer")
        .customerId(customerId)
        .maxLockWaitSeconds(60)
        .maxExpectedDurationSeconds(90)
        .build();
  }

  public static Lock forAgent(@NonNull Long customerId) {
    return Lock.builder()
        .name("agent")
        .tag("agent")
        .customerId(customerId)
        .maxLockWaitSeconds(20)
        .maxExpectedDurationSeconds(5)
        .build();
  }

  public static Lock forPublication(@NonNull File file) {
    return Lock.builder()
        .name(file.getName())
        .tag("publication")
        .maxLockWaitSeconds(0)
        .maxExpectedDurationSeconds(90)
        .build();
  }

  public boolean isTaskLock() {
    return customerId == null || customerId < 0;
  }

  public String key() {
    return isTaskLock()
        ? String.format("codekvast-%s", name)
        : String.format("codekvast-%s-%d", name, customerId);
  }

  public Duration getWaitDuration() {
    return Duration.between(waitStartedAt, acquiredAt);
  }

  public Duration getLockDuration() {
    return Duration.between(acquiredAt, Instant.now());
  }

  public boolean wasLongDuration() {
    return getLockDuration().toSeconds() >= getMaxExpectedDurationSeconds();
  }

  @Override
  public String toString() {
    return String.format("%s(key=%s)", getClass().getSimpleName(), key());
  }
}
