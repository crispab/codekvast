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
package io.codekvast.dashboard.agent.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.AgentPolledEvent;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.Size;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Handler for javaagent REST requests.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentServiceImpl implements AgentService {

    public static final String UNKNOWN_ENVIRONMENT = "<UNKNOWN>";

    private final CodekvastDashboardSettings settings;
    private final CustomerService customerService;
    private final EventService eventService;
    private final AgentDAO agentDAO;

    @Override
    @Transactional
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        val environment = agentDAO.getEnvironmentName(request.getJvmUuid()).orElse(UNKNOWN_ENVIRONMENT);
        val request2 = GetConfigRequest2.fromFormat1(request, environment);
        return GetConfigResponse2.toFormat1(getConfig(request2));
    }

    @Override
    @Transactional
    public GetConfigResponse2 getConfig(GetConfigRequest2 request) throws LicenseViolationException {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey(request.getLicenseKey());

        boolean isAgentEnabled = updateAgentState(customerData, request.getJvmUuid(), request.getAppName(), request.getEnvironment());

        String publisherConfig = isAgentEnabled ? "enabled=true" : "enabled=false";
        PricePlan pp = customerData.getPricePlan();
        return GetConfigResponse2
            .builder()
            .codeBasePublisherCheckIntervalSeconds(pp.getPublishIntervalSeconds())
            .codeBasePublisherConfig(publisherConfig)
            .codeBasePublisherName("http")
            .codeBasePublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .configPollIntervalSeconds(pp.getPollIntervalSeconds())
            .configPollRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .customerId(customerData.getCustomerId())
            .invocationDataPublisherConfig(publisherConfig)
            .invocationDataPublisherIntervalSeconds(pp.getPublishIntervalSeconds())
            .invocationDataPublisherName("http")
            .invocationDataPublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .build();
    }

    private boolean updateAgentState(CustomerData customerData, String jvmUuid, String appName,
                                     @NonNull @Size(min = 1, message = "environment must be at least 1 characters") String environment) {
        long customerId = customerData.getCustomerId();
        Instant now = Instant.now();

        agentDAO.disableDeadAgents(customerId, jvmUuid, now.minusSeconds(settings.getQueuePathPollIntervalSeconds() * 2));

        agentDAO.setAgentTimestamps(customerId, jvmUuid, now, now.plusSeconds(customerData.getPricePlan().getPollIntervalSeconds()));

        CustomerData cd = customerService.registerAgentPoll(customerData, now);
        int numOtherEnabledLiveAgents = agentDAO.getNumOtherAliveAgents(customerId, jvmUuid, now.minusSeconds(10));
        boolean tooManyLiveAgents = numOtherEnabledLiveAgents >= customerData.getPricePlan().getMaxNumberOfAgents();
        boolean disabledEnvironment = !agentDAO.isEnvironmentEnabled(customerId, jvmUuid);
        boolean isTrialPeriodExpired = cd.isTrialPeriodExpired(now);

        val event = AgentPolledEvent.builder()
                                    .customerId(customerId)
                                    .appName(appName)
                                    .environment(environment)
                                    .polledAt(now)
                                    .afterTrialPeriod(isTrialPeriodExpired)
                                    .disabledEnvironment(disabledEnvironment)
                                    .tooManyLiveAgents(tooManyLiveAgents)
                                    .build();

        logger.debug("Agent {} is {}", jvmUuid, event.isAgentEnabled() ? "enabled" : "disable");
        eventService.send(event);
        agentDAO.updateAgentEnabledState(customerId, jvmUuid, event.isAgentEnabled());
        return event.isAgentEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public File savePublication(@NonNull PublicationType publicationType, @NonNull String licenseKey, int publicationSize,
                                InputStream inputStream) throws LicenseViolationException, IOException {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey(licenseKey);
        customerService.assertPublicationSize(customerData, publicationSize);

        return doSaveInputStream(publicationType, customerData.getCustomerId(), inputStream);
    }

    private File doSaveInputStream(PublicationType publicationType, Long customerId, InputStream inputStream) throws IOException {
        try (inputStream) {
            createDirectory(settings.getQueuePath());

            File result = File.createTempFile(publicationType + "-" + customerId + "-", ".ser", settings.getQueuePath());
            Files.copy(inputStream, result.toPath(), REPLACE_EXISTING);

            logger.info("Saved uploaded {} publication to {}", publicationType, result);
            return result;
        }
    }

    private void createDirectory(File queuePath) throws IOException {
        if (!queuePath.isDirectory()) {
            logger.debug("Creating {}", settings.getQueuePath());
            settings.getQueuePath().mkdirs();
            if (!settings.getQueuePath().isDirectory()) {
                throw new IOException("Could not create import directory " + settings.getQueuePath());
            }
            logger.info("Created {}", queuePath);
        }
    }

}
