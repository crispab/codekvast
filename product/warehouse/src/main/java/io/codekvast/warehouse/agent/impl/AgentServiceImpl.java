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
package io.codekvast.warehouse.agent.impl;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.warehouse.agent.AgentService;
import io.codekvast.warehouse.agent.CustomerData;
import io.codekvast.warehouse.agent.LicenseViolationException;
import io.codekvast.warehouse.agent.PricePlan;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Handler for javaagent REST requests.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class AgentServiceImpl implements AgentService {

    private final CodekvastSettings settings;
    private final JdbcTemplate jdbcTemplate;


    @Inject
    public AgentServiceImpl(CodekvastSettings settings, JdbcTemplate jdbcTemplate) {
        this.settings = settings;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        CustomerData customerData = getCustomerData(request.getLicenseKey());
        PricePlan pp = customerData.getPricePlan();

        return GetConfigResponse1
            .builder()
            .codeBasePublisherName("http")
            .codeBasePublisherConfig("enabled=true") // TODO: enforce number of dynos
            .customerId(customerData.getCustomerId())
            .invocationDataPublisherName("http")
            .invocationDataPublisherConfig("enabled=true") // TODO: enforce number of dynos
            .configPollIntervalSeconds(pp.getPollIntervalSeconds())
            .configPollRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .codeBasePublisherCheckIntervalSeconds(pp.getPublishIntervalSeconds())
            .codeBasePublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .invocationDataPublisherIntervalSeconds(pp.getPublishIntervalSeconds())
            .invocationDataPublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
            .build();
    }

    @Override
    public File saveCodeBasePublication(@NonNull String licenseKey, String codeBaseFingerprint, InputStream inputStream)
        throws LicenseViolationException, IOException {
        getCustomerData(licenseKey).getCustomerId();

        return doSaveInputStream(inputStream, "codebase-");
    }

    @Override
    public File saveInvocationDataPublication(@NonNull String licenseKey, String codeBaseFingerprint, InputStream inputStream)
        throws LicenseViolationException, IOException {
        getCustomerData(licenseKey).getCustomerId();

        return doSaveInputStream(inputStream, "invocations-");
    }

    @Override
    public CustomerData getCustomerData(String licenseKey) throws LicenseViolationException {
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT id, plan FROM customers WHERE licenseKey = ?", licenseKey.trim());
            return CustomerData.builder()
                               .customerId((Long) result.get("id"))
                               .planName((String) result.get("plan"))
                               .build();
        } catch (DataAccessException e) {
            throw new LicenseViolationException("Invalid license key: '" + licenseKey + "'");
        }
    }

    private File doSaveInputStream(InputStream inputStream, String prefix) throws IOException {
        createDirectory(settings.getQueuePath());

        File result = File.createTempFile(prefix, ".ser", settings.getQueuePath());
        Files.copy(inputStream, result.toPath(), REPLACE_EXISTING);

        log.debug("Saved uploaded publication to {}", result);
        return result;
    }

    private void createDirectory(File queuePath) throws IOException {
        if (!queuePath.isDirectory()) {
            log.debug("Creating {}", settings.getQueuePath());
            settings.getQueuePath().mkdirs();
            if (!settings.getQueuePath().isDirectory()) {
                throw new IOException("Could not create import directory");
            }
            log.info("Created {}", queuePath);
        }
    }

}
