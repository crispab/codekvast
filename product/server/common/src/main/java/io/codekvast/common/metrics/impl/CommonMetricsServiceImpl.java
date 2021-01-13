/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
package io.codekvast.common.metrics.impl;

import io.codekvast.common.lock.Lock;
import io.codekvast.common.messaging.model.CodekvastEvent;
import io.codekvast.common.metrics.CommonMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

/** @author olle.hallin@crisp.se */
@Component("commonMetricsService")
@RequiredArgsConstructor
@Slf4j
public class CommonMetricsServiceImpl implements CommonMetricsService {

  private static final String EVENT_TAG = "event";
  public static final String LOCK_TAG = "lock";
  public static final String SOURCE_TAG = "source";
  public static final String CONSUMER_TAG = "consumer";

  private final MeterRegistry meterRegistry;

  private final AtomicInteger eventCounter = new AtomicInteger();

  @Override
  public void countApplicationStartup() {
    meterRegistry.counter("codekvast.lifecycle", EVENT_TAG, "startup").increment();
  }

  @Override
  public void countApplicationStarted() {
    meterRegistry.counter("codekvast.lifecycle", EVENT_TAG, "started").increment();
  }

  @Override
  public void countApplicationShutdown() {
    meterRegistry.counter("codekvast.lifecycle", EVENT_TAG, "shutdown").increment();
  }

  @Override
  public void countSentSlackMessage() {
    meterRegistry.counter("codekvast.slack_messages").increment();
  }

  @Override
  public void countLogin(String source) {
    meterRegistry.counter("codekvast.login.count", SOURCE_TAG, source).increment();
  }

  @Override
  public void recordLockWait(Lock lock) {
    meterRegistry
        .timer("codekvast.lock.wait", LOCK_TAG, lock.getTag())
        .record(lock.getWaitDuration());
  }

  @Override
  public void recordLockDuration(Lock lock) {
    meterRegistry
        .timer("codekvast.lock.duration", LOCK_TAG, lock.getTag())
        .record(lock.getLockDuration());
  }

  @Override
  public void countLockFailure(Lock lock) {
    meterRegistry.counter("codekvast.lock.failed", LOCK_TAG, lock.getTag()).increment();
  }

  @Override
  public void recordEventConsumed(String consumerName, CodekvastEvent event, Duration duration) {

    Timer timer =
        meterRegistry.timer(
            "codekvast.event_consumed.duration",
            CONSUMER_TAG,
            consumerName,
            EVENT_TAG,
            event.getClass().getSimpleName());
    timer.record(duration);

    val count = eventCounter.incrementAndGet();

    if (count % 1000 == 0L) {
      logger.info(
          "Received {} events. Processing time max = {} ms, mean = {} ms",
          count,
          formatDouble(timer.max(TimeUnit.MILLISECONDS)),
          formatDouble(timer.mean(TimeUnit.MILLISECONDS)));
    }
  }

  private String formatDouble(Double d) {
    return String.format(Locale.ENGLISH, "%.1f", d);
  }
}
