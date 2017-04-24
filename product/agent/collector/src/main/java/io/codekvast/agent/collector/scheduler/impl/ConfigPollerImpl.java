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
package io.codekvast.agent.collector.scheduler.impl;

import io.codekvast.agent.collector.scheduler.ConfigPoller;
import io.codekvast.agent.collector.io.impl.NoOpCodeBasePublisherImpl;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.rest.GetConfigResponse1;
import io.codekvast.agent.lib.util.ComputerID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class ConfigPollerImpl implements ConfigPoller {
    private final CollectorConfig config;
    private final GetConfigRequest1 requestTemplate;

    @Getter
    private CodeBaseFingerprint codeBaseFingerprint;

    public ConfigPollerImpl(CollectorConfig config) {
        this.config = config;
        this.requestTemplate = GetConfigRequest1.builder()
                                                .appName(config.getAppName())
                                                .appVersion(config.getAppVersion())
                                                .collectorVersion(getCollectorVersion())
                                                .computerId(ComputerID.compute().toString())
                                                .hostName(getHostName())
                                                .jvmUuid(UUID.randomUUID().toString())
                                                .licenseKey(config.getLicenseKey())
                                                .startedAtMillis(System.currentTimeMillis())
                                                .build();
    }

    @Override
    public GetConfigResponse1 doPoll(boolean firstTime) throws Exception {
        this.codeBaseFingerprint = calculateCodeBaseFingerprint(firstTime);

        GetConfigRequest1 request = requestTemplate.toBuilder().codeBaseFingerprint(codeBaseFingerprint.getSha256()).build();
        log.debug("Posting {} to {}", request, config.getConfigRequestEndpoint());

        // TODO: implement proper config polling

        return GetConfigResponse1.builder()
                                 .codeBasePublisherName(NoOpCodeBasePublisherImpl.NAME)
                                 .codeBasePublisherCheckIntervalSeconds(10)
                                 .codeBasePublisherRetryIntervalSeconds(10)
                                 .codeBasePublisherConfig("enabled=true")
                                 .codeBasePublishingNeeded(true)
                                 .build();
    }

    private CodeBaseFingerprint calculateCodeBaseFingerprint(boolean firstTime) {
        return firstTime ? new CodeBase(config).getFingerprint() : null;
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private String getCollectorVersion() {
        return ConfigPollerImpl.class.getPackage().getImplementationVersion();
    }

}
