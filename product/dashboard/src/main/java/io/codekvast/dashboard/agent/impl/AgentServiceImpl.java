/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.dashboard.agent.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
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

    private final CodekvastDashboardSettings settings;
    private final JdbcTemplate jdbcTemplate;
    private final CustomerService customerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey(request.getLicenseKey());

        boolean isAgentEnabled = updateAgentState(customerData, request.getJvmUuid());

        String publisherConfig = isAgentEnabled ? "enabled=true" : "enabled=false";
        PricePlan pp = customerData.getPricePlan();
        return GetConfigResponse1
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

    private boolean updateAgentState(CustomerData customerData, String jvmUuid) {

        long customerId = customerData.getCustomerId();
        Instant now = Instant.now();

        // Disable all agents that have been dead for more than two file import intervals...
        int updated = jdbcTemplate.update("UPDATE agent_state SET enabled = FALSE " +
                                              "WHERE customerId = ? AND nextPollExpectedAt < ? AND enabled = TRUE ",
                                          customerId, Timestamp.from(now.minusSeconds(settings.getQueuePathPollIntervalSeconds() * 2)));
        if (updated > 0) {
            logger.info("Disabled {} dead agents for {}", updated, customerData);
        }

        Timestamp nextExpectedPollTimestamp = Timestamp.from(now.plusSeconds(customerData.getPricePlan().getPollIntervalSeconds()));
        updated =
            jdbcTemplate.update("UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ? WHERE customerId = ? AND jvmUuid = ?",
                                Timestamp.from(now), nextExpectedPollTimestamp, customerId, jvmUuid);
        if (updated == 0) {
            logger.info("The agent {}:{} has started", customerId, jvmUuid);

            jdbcTemplate
                .update("INSERT INTO agent_state(customerId, jvmUuid, lastPolledAt, nextPollExpectedAt, enabled) VALUES (?, ?, ?, ?, ?)",
                        customerId, jvmUuid, Timestamp.from(now), nextExpectedPollTimestamp, Boolean.TRUE);
        } else {
            logger.debug("The agent {}:{} has polled", customerId, jvmUuid);
        }

        Integer numOtherEnabledLiveAgents =
            jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_state " +
                                            "WHERE enabled = TRUE AND customerId = ? AND nextPollExpectedAt >= ? AND jvmUuid != ? ",
                                        Integer.class, customerId, Timestamp.from(now.minusSeconds(10)), jvmUuid);

        String planName = customerData.getPricePlan().getName();
        int maxNumberOfAgents = customerData.getPricePlan().getMaxNumberOfAgents();
        boolean enabled = numOtherEnabledLiveAgents < maxNumberOfAgents;
        if (!enabled) {
            logger.warn("Customer {} has already {} live agents (max for price plan '{}' is {})", customerId, numOtherEnabledLiveAgents,
                        planName, maxNumberOfAgents);
        } else {
            logger.debug("Customer {} now has {} live agents (max for price plan '{}' is {})", customerId, numOtherEnabledLiveAgents + 1,
                         planName, maxNumberOfAgents);
        }

        jdbcTemplate.update("UPDATE agent_state SET enabled = ? WHERE jvmUuid = ?", enabled, jvmUuid);
        CustomerData cd = customerService.registerAgentDataPublication(customerData, now);
        if (cd.isTrialPeriodExpired(now)) {
            logger.info("Trial period expired for {}", cd);
            enabled = false;
        }
        return enabled;
    }

    @Override
    public File savePublication(@NonNull PublicationType publicationType, @NonNull String licenseKey, int publicationSize,
                                InputStream inputStream) throws LicenseViolationException, IOException {

        customerService.assertPublicationSize(licenseKey, publicationSize);

        return doSaveInputStream(publicationType, inputStream);
    }

    private File doSaveInputStream(PublicationType publicationType, InputStream inputStream) throws IOException {
        createDirectory(settings.getQueuePath());

        File result = File.createTempFile(publicationType + "-", ".ser", settings.getQueuePath());
        Files.copy(inputStream, result.toPath(), REPLACE_EXISTING);

        logger.debug("Saved uploaded {} publication to {}", publicationType, result);
        return result;
    }

    private void createDirectory(File queuePath) throws IOException {
        if (!queuePath.isDirectory()) {
            logger.debug("Creating {}", settings.getQueuePath());
            settings.getQueuePath().mkdirs();
            if (!settings.getQueuePath().isDirectory()) {
                throw new IOException("Could not create import directory");
            }
            logger.info("Created {}", queuePath);
        }
    }

}
