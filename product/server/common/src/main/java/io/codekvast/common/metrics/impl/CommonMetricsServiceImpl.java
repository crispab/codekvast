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
package io.codekvast.common.metrics.impl;

import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author olle.hallin@crisp.se
 */
@Component("commonMetricsService")
@RequiredArgsConstructor
public class CommonMetricsServiceImpl implements CommonMetricsService {

    private static final String EVENT_TAG = "event";
    public static final String LOCK_TAG = "lock";

    private final MeterRegistry meterRegistry;

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
        meterRegistry.counter("codekvast.login.count", "source", source).increment();
        meterRegistry.counter("codekvast.login.count" + "." + source).increment();
    }

    @Override
    public void recordLockDuration(LockManager.Lock lock, Duration duration) {
        meterRegistry.counter("codekvast.lock.acquired", LOCK_TAG, lock.name()).increment();
        meterRegistry.timer("codekvast.lock.duration.millis", LOCK_TAG, lock.name()).record(duration);
    }

    @Override
    public void recordLockWait(LockManager.Lock lock, Duration duration) {
        meterRegistry.timer("codekvast.lock.wait.millis", LOCK_TAG, lock.name()).record(duration);
    }

    @Override
    public void countLockFailure(LockManager.Lock lock) {
        meterRegistry.counter("codekvast.lock.failed", LOCK_TAG, lock.name()).increment();
    }

}
