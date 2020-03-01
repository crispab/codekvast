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
package io.codekvast.common.metrics;

import io.codekvast.common.lock.Lock;
import io.codekvast.common.messaging.model.CodekvastEvent;
import java.time.Duration;

/**
 * Base interface for metrics-related services
 *
 * @author olle.hallin@crisp.se
 */
public interface CommonMetricsService {

  /**
   * Counts an application startup attempt.
   *
   * <p>NOTE: The counter is incremented early in the startup sequence, so there is no guarantee
   * that the application manages to start successfully.
   */
  void countApplicationStartup();

  /**
   * Counts a finished application startup sequence.
   *
   * <p>NOTE: The counter is incremented only when the application is ready to receive traffic.
   */
  void countApplicationStarted();

  /** Counts shutdown events. */
  void countApplicationShutdown();

  void countSentSlackMessage();

  /**
   * Counts a login
   *
   * @param source The authentication source, e.g., "google", "github" etc.
   */
  void countLogin(String source);

  /**
   * Records how a lock was used.
   *
   * @param lock The lock
   */
  void recordLockUsage(Lock lock);

  /**
   * Count a failure to get a lock.
   *
   * @param lock The lock
   */
  void countLockFailure(Lock lock);

  /**
   * Record the time required for consuming one event.
   *
   * @param consumerName The name of the consumer
   * @param event The type of event
   * @param duration The time it took to process the event
   */
  void recordEventConsumed(String consumerName, CodekvastEvent event, Duration duration);
}
