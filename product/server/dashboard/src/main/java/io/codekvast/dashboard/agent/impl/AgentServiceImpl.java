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

import io.codekvast.common.aspects.Restartable;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.messaging.CorrelationIdHolder;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.PublicationMetricsService;
import io.codekvast.dashboard.model.PublicationType;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.codekvast.common.util.LoggingUtils.humanReadableByteCount;
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
    public static final Pattern CORRELATION_ID_PATTERN = buildCorrelationIdPattern();

    private final CodekvastDashboardSettings settings;
    private final CustomerService customerService;
    private final AgentDAO agentDAO;
    private final AgentStateManager agentStateManager;
    private final PublicationMetricsService publicationMetricsService;

    @Override
    @Restartable
    @Transactional
    public GetConfigResponse1 getConfig(GetConfigRequest1 request) throws LicenseViolationException {
        val environment = agentDAO.getEnvironmentName(request.getJvmUuid()).orElse(UNKNOWN_ENVIRONMENT);
        val request2 = GetConfigRequest2.fromFormat1(request, environment);
        return GetConfigResponse2.toFormat1(getConfig(request2));
    }

    @Override
    @Restartable
    @Transactional
    public GetConfigResponse2 getConfig(GetConfigRequest2 request) throws LicenseViolationException {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey(request.getLicenseKey());

        boolean isAgentEnabled =
            agentStateManager.updateAgentState(customerData, request.getJvmUuid(), request.getAppName(), request.getEnvironment());

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


    @Override
    @Transactional(readOnly = true)
    public File savePublication(@NonNull PublicationType publicationType, @NonNull String licenseKey, String codebaseFingerprint,
                                int publicationSize, InputStream inputStream) throws LicenseViolationException, IOException {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey(licenseKey);
        if (publicationType == PublicationType.CODEBASE) {
            if (agentDAO.isCodebaseAlreadyImported(customerData.getCustomerId(), codebaseFingerprint)) {
                logger.info("Ignoring duplicate {} with fingerprint {} for customer {}.", publicationType, codebaseFingerprint,
                            customerData.getCustomerId());
                publicationMetricsService.countIgnoredPublication(publicationType);
                inputStream.close();
                return null;
            }
            customerService.assertPublicationSize(customerData, publicationSize);
        }

        return doSaveInputStream(publicationType, customerData.getCustomerId(), codebaseFingerprint, inputStream);
    }

    @Override
    public File generatePublicationFile(PublicationType publicationType, Long customerId, String correlationId) {
        return new File(settings.getFileImportQueuePath(), String.format("%s-%d-%s.ser", publicationType, customerId, correlationId));
    }

    private static Pattern buildCorrelationIdPattern() {
        String publicationTypes =
            Arrays.stream(PublicationType.values()).map(PublicationType::toString).collect(Collectors.joining("|", "(", ")"));
        return Pattern.compile(publicationTypes + "-([0-9]+)-([a-fA-F0-9_-]+)\\.ser$");
    }

    @Override
    public PublicationType getPublicationTypeFromPublicationFile(File publicationFile) {
        String fileName = publicationFile.getName();
        Matcher matcher = CORRELATION_ID_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return PublicationType.valueOf(matcher.group(1).toUpperCase());
        }
        logger.warn("Could not parse publicationType from publication file name {}", fileName);
        return null;
    }

    @Override
    public String getCorrelationIdFromPublicationFile(File publicationFile) {
        String fileName = publicationFile.getName();
        Matcher matcher = CORRELATION_ID_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        logger.warn("Could not parse correlationId from publication file name {}, generating a new...", fileName);
        return CorrelationIdHolder.generateNew();
    }

    private File doSaveInputStream(PublicationType publicationType, Long customerId, String codebaseFingerprint, InputStream inputStream)
        throws IOException {
        try (inputStream) {
            createDirectory(settings.getFileImportQueuePath());

            File result = generatePublicationFile(publicationType, customerId, CorrelationIdHolder.get());
            Files.copy(inputStream, result.toPath(), REPLACE_EXISTING);

            logger
                .info("Saved uploaded {} publication for customer {} to {} ({}), fingerprint = {}", publicationType, customerId, result,
                      humanReadableByteCount(result.length()), codebaseFingerprint);
            return result;
        }
    }

    private void createDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            logger.debug("Creating {}", directory);
            directory.mkdirs();
            if (!directory.isDirectory()) {
                throw new IOException("Could not create directory " + directory);
            }
            logger.info("Created {}", directory);
        }
    }

}
