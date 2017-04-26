/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.agent.collector.scheduler;

import io.codekvast.agent.collector.CodekvastThreadFactory;
import io.codekvast.agent.collector.InvocationRegistry;
import io.codekvast.agent.collector.io.CodeBasePublisher;
import io.codekvast.agent.collector.io.CodeBasePublisherFactory;
import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.collector.io.InvocationDataPublisherFactory;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;
import io.codekvast.agent.lib.util.LogUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for executing recurring tasks within the collector.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ClassWithTooManyFields")
@Slf4j
public class Scheduler implements Runnable {
    // Collaborators
    private final CollectorConfig config;
    private final ConfigPoller configPoller;
    private final CodeBasePublisherFactory codeBasePublisherFactory;
    private final InvocationDataPublisherFactory invocationDataPublisherFactory;
    private final ScheduledExecutorService executor;

    // Mutable state
    private GetConfigResponse1 dynamicConfig;
    private final SchedulerState pollState = new SchedulerState().initialize(10, 10);

    private final SchedulerState codeBasePublisherState = new SchedulerState().initialize(10, 10);
    private CodeBasePublisher codeBasePublisher;
    private CodeBaseFingerprint codeBaseFingerprint;

    private final SchedulerState invocationDataPublisherState = new SchedulerState().initialize(10, 10);
    private InvocationDataPublisher invocationDataPublisher;

    public Scheduler(CollectorConfig config,
                     ConfigPoller configPoller,
                     CodeBasePublisherFactory codeBasePublisherFactory,
                     InvocationDataPublisherFactory invocationDataPublisherFactory) {
        this.config = config;
        this.configPoller = configPoller;
        this.codeBasePublisherFactory = codeBasePublisherFactory;
        this.invocationDataPublisherFactory = invocationDataPublisherFactory;
        this.executor = Executors.newScheduledThreadPool(1, new CodekvastThreadFactory());
    }

    /**
     * Starts the scheduler.
     *
     * @return this
     */
    public Scheduler start(long delay, long period, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(this, delay, period, timeUnit);
        log.info("Scheduler started; pulling dynamic config from {}", config.getServerUrl());
        return this;
    }

    /**
     * Shuts down the scheduler. Performs a last invocation data publishing before returning.
     */
    public void shutdown() {
        log.info("Stopping scheduler");
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug("Stop interrupted");
        }

        if (dynamicConfig != null) {
            // We have done at least one successful poll

            // Make sure the last data is published...
            codeBasePublisherState.scheduleNow();
            publishCodeBaseIfNeeded();

            invocationDataPublisherState.scheduleNow();
            publishInvocationDataIfNeeded();
        }
    }

    @Override
    public void run() {
        if (executor.isShutdown()) {
            log.debug("Shutting down");
            return;
        }

        pollDynamicConfigIfNeeded();
        publishCodeBaseIfNeeded();
        publishInvocationDataIfNeeded();
    }

    private void pollDynamicConfigIfNeeded() {
        if (pollState.isDueTime()) {
            log.trace("Polling dynamic config");
            try {
                dynamicConfig = configPoller.doPoll(pollState.isFirstTime());
                if (pollState.isFirstTime()) {
                    codeBaseFingerprint = configPoller.getCodeBaseFingerprint();
                }

                configureCodeBasePublisher(dynamicConfig.isCodeBasePublishingNeeded());
                configureInvocationDataPublisher();

                pollState.updateIntervals(dynamicConfig.getConfigPollIntervalSeconds(), dynamicConfig.getConfigPollRetryIntervalSeconds());
                pollState.scheduleNext();
            } catch (Exception e) {
                log.error("Failed to poll " + config.getPollConfigRequestEndpoint(), e);

                pollState.scheduleRetry();
            }
        }
    }

    private void configureCodeBasePublisher(boolean isCodeBasePublishingNeeded) {
        codeBasePublisherState.updateIntervals(dynamicConfig.getCodeBasePublisherCheckIntervalSeconds(),
                                               dynamicConfig.getCodeBasePublisherRetryIntervalSeconds());

        String newName = dynamicConfig.getCodeBasePublisherName();
        if (codeBasePublisher == null || !newName.equals(codeBasePublisher.getName())) {
            codeBasePublisher = codeBasePublisherFactory.create(newName, config);
            if (isCodeBasePublishingNeeded) {
                codeBasePublisher.initialize(null);
                codeBasePublisherState.scheduleNow();
            } else {
                codeBasePublisher.initialize(codeBaseFingerprint);
                codeBasePublisherState.scheduleNext();
            }
        }
        codeBasePublisher.configure(dynamicConfig.getCodeBasePublisherConfig());
    }

    private void configureInvocationDataPublisher() {
        invocationDataPublisherState.updateIntervals(dynamicConfig.getInvocationDataPublisherRetryIntervalSeconds(),
                                                     dynamicConfig.getInvocationDataPublisherRetryIntervalSeconds());

        String newName = dynamicConfig.getInvocationDataPublisherName();
        if (invocationDataPublisher == null || !newName.equals(invocationDataPublisher.getName())) {
            invocationDataPublisher = invocationDataPublisherFactory.create(newName, config);
            invocationDataPublisherState.scheduleNext();
        }

        if (codeBasePublisher != null) {
            invocationDataPublisher.setCodeBaseFingerprint(codeBasePublisher.getCodeBaseFingerprint());
        }

        invocationDataPublisher.configure(dynamicConfig.getInvocationDataPublisherConfig());
    }

    private void publishCodeBaseIfNeeded() {
        if (codeBasePublisherState.isDueTime() && dynamicConfig != null) {
            try {
                codeBasePublisher.publishCodeBase();
                codeBaseFingerprint = codeBasePublisher.getCodeBaseFingerprint();
                codeBasePublisherState.scheduleNext();
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to publish code base", e);
                codeBasePublisherState.scheduleRetry();
            }
        }
    }

    private void publishInvocationDataIfNeeded() {
        if (invocationDataPublisherState.isDueTime() && dynamicConfig != null) {
            try {
                InvocationRegistry.instance.publishInvocationData(invocationDataPublisher);
                invocationDataPublisherState.scheduleNext();
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to publish invocation data", e);
                invocationDataPublisherState.scheduleRetry();
            }
        }
    }

    @Getter
    static class SchedulerState {
        private long nextEventAtMillis;
        private int intervalSeconds;
        private int retryIntervalSeconds;
        private int retryIntervalFactor;
        private int numFailures;
        private boolean firstTime;

        SchedulerState initialize(int intervalSeconds, int retryIntervalSeconds) {
            this.intervalSeconds = intervalSeconds;
            this.retryIntervalSeconds = retryIntervalSeconds;
            this.nextEventAtMillis = 0L;
            this.firstTime = true;
            resetRetryCounter();
            return this;
        }

        private void resetRetryCounter() {
            this.numFailures = 0;
            this.retryIntervalFactor = 1;
        }

        void updateIntervals(int intervalSeconds, int retryIntervalSeconds) {
            this.intervalSeconds = intervalSeconds;
            this.retryIntervalSeconds = retryIntervalSeconds;
            scheduleNext();
        }

        void scheduleNext() {
            nextEventAtMillis = System.currentTimeMillis() + intervalSeconds * 1000L;
            firstTime = false;
            resetRetryCounter();
        }

        void scheduleNow() {
            nextEventAtMillis = 0L;
        }

        void scheduleRetry() {
            int backOffLimit = 5;

            if (numFailures < backOffLimit) {
                retryIntervalFactor = 1;
            } else {
                retryIntervalFactor = (int) Math.pow(2, Math.min(numFailures - backOffLimit + 1, 4));
            }
            nextEventAtMillis = System.currentTimeMillis() + retryIntervalSeconds * retryIntervalFactor * 1000L;
            numFailures += 1;
        }

        boolean isDueTime() {
            return System.currentTimeMillis() >= nextEventAtMillis;
        }

    }
}
