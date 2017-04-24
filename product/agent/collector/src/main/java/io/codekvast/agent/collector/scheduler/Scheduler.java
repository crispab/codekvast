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
import io.codekvast.agent.collector.io.impl.CodeBasePublisherFactory;
import io.codekvast.agent.collector.scheduler.impl.ConfigPollerImpl;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.rest.GetConfigResponse1;
import io.codekvast.agent.lib.util.LogUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for executing recurring tasks within the collector.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class Scheduler implements Runnable {
    // Collaborators
    private final CollectorConfig config;
    private final ConfigPollerImpl configPoller;
    private final ScheduledExecutorService executor;

    // Mutable state
    private GetConfigResponse1 dynamicConfig;

    private long nextConfigPollAtMillis = 0L;
    private boolean firstConfigPoll = true;

    private long nextCodeBaseCheckAtMillis = 0L;
    private CodeBasePublisher codeBasePublisher;

    private long nextInvocationDataPublishingAtMillis = 0L;
    private int invocationDataPublishCount;

    public Scheduler(CollectorConfig config, ConfigPollerImpl configPoller) {
        this.config = config;
        this.configPoller = configPoller;
        this.executor = Executors.newScheduledThreadPool(1, new CodekvastThreadFactory());
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

    public void shutdown() {
        log.info("Stopping scheduler");
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug("Stop interrupted");
        }

        nextInvocationDataPublishingAtMillis = 0L;
        doInvocationDataPublishing();
    }

    @Override
    public void run() {
        if (executor.isShutdown()) {
            return;
        }

        doConfigPoll();
        checkIfCodeBaseNeedsPublishing();
        doInvocationDataPublishing();
    }

    private void doConfigPoll() {
        if (System.currentTimeMillis() >= nextConfigPollAtMillis) {
            log.trace("Doing config poll");
            try {
                dynamicConfig = configPoller.doPoll(firstConfigPoll);

                configureCodeBasePublisher(configPoller.getCodeBaseFingerprint());
                configureInvocationDataPublisher(firstConfigPoll);

                scheduleNextPollAt(dynamicConfig.getConfigPollIntervalSeconds());
                firstConfigPoll = false;
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to poll " + config.getConfigRequestEndpoint(), e);

                // TODO: implement some back-off algorithm to prevent spamming server
                scheduleNextPollAt(dynamicConfig.getConfigPollRetryIntervalSeconds());
            }
        }
    }

    private void configureInvocationDataPublisher(boolean firstTime) {
        // TODO: make configurable from server
        if (firstTime) {
            nextInvocationDataPublishingAtMillis = scheduleDelay(5);
        }
    }

    private void configureCodeBasePublisher(CodeBaseFingerprint codeBaseFingerprint) {
        String newName = dynamicConfig.getCodeBasePublisherName();
        if (codeBasePublisher == null || !codeBasePublisher.getName().equals(newName)) {
            codeBasePublisher = CodeBasePublisherFactory.create(config, newName);
            codeBasePublisher.setCodeBaseFingerprint(codeBaseFingerprint);
        }
        codeBasePublisher.configure(dynamicConfig.getCodeBasePublisherConfig());
    }

    private void checkIfCodeBaseNeedsPublishing() {
        if (System.currentTimeMillis() >= nextCodeBaseCheckAtMillis) {
            try {
                codeBasePublisher.publishCodebase();
                scheduleNextCodeBaseCheck(dynamicConfig.getConfigPollIntervalSeconds());
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to publish code base", e);

                // TODO: implement some back-off algorithm to prevent spamming server
                scheduleNextCodeBaseCheck(dynamicConfig.getConfigPollRetryIntervalSeconds());
            }
        }
    }

    private void scheduleNextCodeBaseCheck(int delaySeconds) {
        nextCodeBaseCheckAtMillis = scheduleDelay(delaySeconds);
    }

    private void scheduleNextPollAt(int delaySeconds) {
        nextConfigPollAtMillis = scheduleDelay(delaySeconds);
    }

    private void doInvocationDataPublishing() {
        if (System.currentTimeMillis() > nextInvocationDataPublishingAtMillis) {
            invocationDataPublishCount += 1;

            // TODO: refactor
            log.debug("Publishing invocation data #{}", invocationDataPublishCount);
            InvocationRegistry.instance.publishData(invocationDataPublishCount);

            nextInvocationDataPublishingAtMillis = scheduleDelay(config.getCollectorResolutionSeconds());
        }

    }

    private long scheduleDelay(int delaySeconds) {
        return System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delaySeconds, TimeUnit.SECONDS);
    }

}
