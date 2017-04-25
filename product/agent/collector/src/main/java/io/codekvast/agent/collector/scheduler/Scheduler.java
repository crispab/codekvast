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
import io.codekvast.agent.collector.io.*;
import io.codekvast.agent.collector.io.impl.FileSystemInvocationDataPublisherImpl;
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
    private final ConfigPoller configPoller;
    private final CodeBasePublisherFactory codeBasePublisherFactory;
    private final InvocationDataPublisherFactory invocationDataPublisherFactory;
    private final ScheduledExecutorService executor;

    // Mutable state
    private GetConfigResponse1 dynamicConfig;

    private long nextConfigPollAtMillis = 0L;
    private boolean firstTime = true;

    private long nextCodeBaseCheckAtMillis = 0L;
    private CodeBasePublisher codeBasePublisher;

    private long nextInvocationDataPublishingAtMillis = 0L;
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
    public Scheduler start(long period, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(this, period, period, timeUnit);
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

        // Shutting down before first config poll...
        if (dynamicConfig == null) {
            dynamicConfig = GetConfigResponse1.sample()
                                              .toBuilder()
                                              .invocationDataPublisherName(FileSystemInvocationDataPublisherImpl.NAME)
                                              .invocationDataPublisherConfig("")
                                              .build();
            configureInvocationDataPublisher(true);
        }
        nextInvocationDataPublishingAtMillis = 0L;
        publishInvocationDataIfNeeded();
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
        if (System.currentTimeMillis() >= nextConfigPollAtMillis) {
            log.trace("Polling dynamic config");
            try {
                dynamicConfig = configPoller.doPoll(firstTime);

                configureCodeBasePublisher(
                    dynamicConfig.isCodeBasePublishingNeeded() ? null : configPoller.getCodeBaseFingerprint());

                configureInvocationDataPublisher(firstTime);

                scheduleNextPollAt(dynamicConfig.getConfigPollIntervalSeconds());
                firstTime = false;
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to poll " + config.getConfigRequestEndpoint(), e);

                // TODO: implement some back-off algorithm to prevent spamming server
                scheduleNextPollAt(dynamicConfig.getConfigPollRetryIntervalSeconds());
            }
        }
    }

    private void configureCodeBasePublisher(CodeBaseFingerprint codeBaseFingerprint) {
        String newName = dynamicConfig.getCodeBasePublisherName();
        if (codeBasePublisher == null || !newName.equals(codeBasePublisher.getName())) {
            codeBasePublisher = codeBasePublisherFactory.create(newName, config);
            codeBasePublisher.initialize(codeBaseFingerprint);
        }
        codeBasePublisher.configure(dynamicConfig.getCodeBasePublisherConfig());
    }

    private void configureInvocationDataPublisher(boolean firstTime) {
        String newName = dynamicConfig.getInvocationDataPublisherName();
        if (invocationDataPublisher == null || !newName.equals(invocationDataPublisher.getName())) {
            invocationDataPublisher = invocationDataPublisherFactory.create(newName, config);
        }
        invocationDataPublisher.configure(dynamicConfig.getInvocationDataPublisherConfig());
        if (firstTime) {
            nextInvocationDataPublishingAtMillis = scheduleFromNow(5);
        }
    }

    private void publishCodeBaseIfNeeded() {
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
        nextCodeBaseCheckAtMillis = scheduleFromNow(delaySeconds);
    }

    private void scheduleNextPollAt(int delaySeconds) {
        nextConfigPollAtMillis = scheduleFromNow(delaySeconds);
    }

    private void publishInvocationDataIfNeeded() {
        if (System.currentTimeMillis() > nextInvocationDataPublishingAtMillis) {
            try {
                InvocationRegistry.instance.publishInvocationData(invocationDataPublisher);

                nextInvocationDataPublishingAtMillis = scheduleFromNow(dynamicConfig.getInvocationDataPublisherIntervalSeconds());
            } catch (Exception e) {
                LogUtil.logException(log, "Failed to publish invocation data", e);

                // TODO: implement some back-off algorithm to prevent spamming server
                scheduleNextCodeBaseCheck(dynamicConfig.getInvocationDataPublisherRetryIntervalSeconds());

            }
        }

    }

    private long scheduleFromNow(int delaySeconds) {
        return System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delaySeconds, TimeUnit.SECONDS);
    }

}
