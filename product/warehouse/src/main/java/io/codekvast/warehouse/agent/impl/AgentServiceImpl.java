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
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.LicenseViolationException;
import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
    private final CustomerService customerService;

    @Inject
    public AgentServiceImpl(CodekvastSettings settings, CustomerService customerService) {
        this.settings = settings;
        this.customerService = customerService;
    }

    @Override
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        long customerId = customerService.checkLicenseKeyAndGetCustomerId(request.getLicenseKey());

        // TODO: pick values from CustomerService
        return GetConfigResponse1.builder()
                                 .codeBasePublisherName("http")
                                 .codeBasePublisherConfig("enabled=true")
                                 .customerId(customerId)
                                 .invocationDataPublisherName("http")
                                 .invocationDataPublisherConfig("enabled=true")
                                 .configPollIntervalSeconds(5)
                                 .configPollRetryIntervalSeconds(5)
                                 .codeBasePublisherCheckIntervalSeconds(5)
                                 .codeBasePublisherRetryIntervalSeconds(5)
                                 .invocationDataPublisherIntervalSeconds(5)
                                 .invocationDataPublisherRetryIntervalSeconds(5)
                                 .build();
    }

    @Override
    public File saveCodeBasePublication(@NonNull String licenseKey, String codeBaseFingerprint, InputStream inputStream)
        throws LicenseViolationException, IOException {
        customerService.checkLicenseKeyAndGetCustomerId(licenseKey);

        return doSaveInputStream(inputStream, "codebase-");
    }

    @Override
    public File saveInvocationDataPublication(@NonNull String licenseKey, String codeBaseFingerprint, InputStream inputStream)
        throws LicenseViolationException, IOException {
        customerService.checkLicenseKeyAndGetCustomerId(licenseKey);

        return doSaveInputStream(inputStream, "invocations-");
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
