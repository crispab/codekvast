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
package io.codekvast.javaagent.scheduler;

import io.codekvast.javaagent.CodekvastThreadFactory;
import io.codekvast.javaagent.InvocationRegistry;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import io.codekvast.javaagent.util.LogUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for executing recurring tasks within the agent.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("ClassWithTooManyFields")
@Slf4j
public class Scheduler implements Runnable {
    // Collaborators
    private final AgentConfig config;
    private final ConfigPoller configPoller;
    private final CodeBasePublisherFactory codeBasePublisherFactory;
    private final InvocationDataPublisherFactory invocationDataPublisherFactory;
    private final ScheduledExecutorService executor;

    // Mutable state
    private GetConfigResponse1 dynamicConfig;
    private final SchedulerState pollState = new SchedulerState("configPoll").initialize(10, 10);

    private final SchedulerState codeBasePublisherState = new SchedulerState("codeBase").initialize(10, 10);
    private CodeBasePublisher codeBasePublisher;

    private final SchedulerState invocationDataPublisherState = new SchedulerState("invocationData").initialize(10, 10);
    private InvocationDataPublisher invocationDataPublisher;

    public Scheduler(AgentConfig config,
                     ConfigPoller configPoller,
                     CodeBasePublisherFactory codeBasePublisherFactory,
                     InvocationDataPublisherFactory invocationDataPublisherFactory) {
        this.config = config;
        this.configPoller = configPoller;
        this.codeBasePublisherFactory = codeBasePublisherFactory;
        this.invocationDataPublisherFactory = invocationDataPublisherFactory;
        this.executor = Executors.newScheduledThreadPool(1,
                                                         CodekvastThreadFactory.builder()
                                                                               .name("scheduler")
                                                                               .relativePriority(-1)
                                                                               .build());
    }

    /**
     * Starts the scheduler.
     *
     * @return this
     */
    public Scheduler start() {
        executor.scheduleAtFixedRate(this, 10L, 10L, TimeUnit.SECONDS);
        log.info("Scheduler started; pulling dynamic config from {}", config.getServerUrl());
        return this;
    }

    /**
     * Shuts down the scheduler. Performs a last invocation data publishing before returning.
     */
    public void shutdown() {
        long startedAt = System.currentTimeMillis();

        log.debug("Stopping scheduler");
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug("Stop interrupted");
        }

        if (dynamicConfig != null) {
            // We have done at least one successful poll

            // Make sure the last invocation data is published...
            if (invocationDataPublisher.getCodeBaseFingerprint() == null) {
                // CodeBasePublisher has not executed yet
                codeBasePublisherState.scheduleNow();
                publishCodeBaseIfNeeded();
                invocationDataPublisher.setCodeBaseFingerprint(codeBasePublisher.getCodeBaseFingerprint());
            }
            invocationDataPublisherState.scheduleNow();
            publishInvocationDataIfNeeded();
        }

        log.info("Scheduler stopped in {} ms", System.currentTimeMillis() - startedAt);
    }

    @Override
    public void run() {
        if (executor.isShutdown()) {
            log.debug("Scheduler is shutting down");
            return;
        }

        pollDynamicConfigIfNeeded();
        publishCodeBaseIfNeeded();
        publishInvocationDataIfNeeded();
    }

    private void pollDynamicConfigIfNeeded() {
        if (pollState.isDueTime()) {
            try {
                dynamicConfig = configPoller.doPoll();

                configureCodeBasePublisher();
                configureInvocationDataPublisher();

                pollState.updateIntervals(dynamicConfig.getConfigPollIntervalSeconds(), dynamicConfig.getConfigPollRetryIntervalSeconds());
                pollState.scheduleNext();
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to poll " + config.getPollConfigRequestEndpoint(), e);
                pollState.scheduleRetry();
            }
        }
    }

    private void configureCodeBasePublisher() {
        codeBasePublisherState.updateIntervals(dynamicConfig.getCodeBasePublisherCheckIntervalSeconds(),
                                               dynamicConfig.getCodeBasePublisherRetryIntervalSeconds());

        String newName = dynamicConfig.getCodeBasePublisherName();
        if (codeBasePublisher == null || !newName.equals(codeBasePublisher.getName())) {
            codeBasePublisher = codeBasePublisherFactory.create(newName, config);
            codeBasePublisherState.scheduleNext();
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
            log.debug("Checking if code base needs to be published...");

            try {
                codeBasePublisher.publishCodeBase();
                codeBasePublisherState.scheduleNext();
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to publish code base", e);
                codeBasePublisherState.scheduleRetry();
            }
        }
    }

    private void publishInvocationDataIfNeeded() {
        if (invocationDataPublisherState.isDueTime() && dynamicConfig != null) {
            log.debug("Checking if invocation data needs to be published...");

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
    @RequiredArgsConstructor
    @Slf4j
    static class SchedulerState {
        private final String name;

        private long nextEventAtMillis;
        private int intervalSeconds;
        private int retryIntervalSeconds;
        private int retryIntervalFactor;
        private int numFailures;

        SchedulerState initialize(int intervalSeconds, int retryIntervalSeconds) {
            this.intervalSeconds = intervalSeconds;
            this.retryIntervalSeconds = retryIntervalSeconds;
            this.nextEventAtMillis = 0L;
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
        }

        void scheduleNext() {
            nextEventAtMillis = System.currentTimeMillis() + intervalSeconds * 1000L;
            if (numFailures > 0) {
                log.debug("{} is exiting failure state after {} failures", name, numFailures);
            }
            resetRetryCounter();
            log.debug("{} will execute next at {}", name, new Date(nextEventAtMillis));
        }

        void scheduleNow() {
            nextEventAtMillis = 0L;
            log.debug("{} will execute now at {}", name, new Date(nextEventAtMillis));
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

            log.debug("{} has failed {} times, will retry at {}", name, numFailures, new Date(nextEventAtMillis));
        }

        boolean isDueTime() {
            return System.currentTimeMillis() >= nextEventAtMillis;
        }
    }
}
