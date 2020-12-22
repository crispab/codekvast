/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.intake.service.impl

import io.codekvast.common.aspects.Restartable
import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.customer.PricePlan
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.messaging.CorrelationIdHolder
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.model.PublicationType
import io.codekvast.intake.service.IntakeService
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1
import io.codekvast.javaagent.model.v2.GetConfigRequest2
import io.codekvast.javaagent.model.v2.GetConfigResponse2
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors


/**
 * @author olle.hallin@crisp.se
 */
@Service
class IntakeServiceImpl(
    private val settings: CodekvastIntakeSettings,
    private val customerService: CustomerService,
    private val intakeDAO: IntakeDAO,
    private val agentStateManager: AgentStateManager,
    private val metricsService: IntakeMetricsService
) : IntakeService {

    val UNKNOWN_ENVIRONMENT = "<UNKNOWN>"
    val CORRELATION_ID_PATTERN = buildCorrelationIdPattern()

    val logger by LoggerDelegate()

    private fun buildCorrelationIdPattern(): Pattern {
        val publicationTypes: String =
            Arrays.stream(PublicationType.values())
                .map(PublicationType::toString)
                .collect(Collectors.joining("|", "(", ")"))
        return Pattern.compile("""$publicationTypes-([0-9]+)-([a-fA-F0-9_-]+)\.ser$""")
    }

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
    override fun getConfig1(request: GetConfigRequest1): GetConfigResponse1 {
        val environment = intakeDAO.getEnvironmentName(request.jvmUuid).orElse(UNKNOWN_ENVIRONMENT)
        val request2 = GetConfigRequest2.fromFormat1(request, environment)
        return GetConfigResponse2.toFormat1(getConfig2(request2))
    }

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
    override fun getConfig2(request: GetConfigRequest2): GetConfigResponse2 {
        metricsService.countAgentPoll()

        val customerData = customerService.getCustomerDataByLicenseKey(request.licenseKey)

        val isAgentEnabled: Boolean = agentStateManager.updateAgentState(
            customerData, request.jvmUuid, request.appName, request.environment
        )

        val publisherConfig = if (isAgentEnabled) "enabled=true" else "enabled=false"
        val pp = customerData.pricePlan
        return GetConfigResponse2.builder()
            .codeBasePublisherCheckIntervalSeconds(pp.publishIntervalSeconds)
            .codeBasePublisherConfig(publisherConfig)
            .codeBasePublisherName("http")
            .codeBasePublisherRetryIntervalSeconds(pp.retryIntervalSeconds)
            .configPollIntervalSeconds(pp.pollIntervalSeconds)
            .configPollRetryIntervalSeconds(pp.retryIntervalSeconds)
            .customerId(customerData.customerId)
            .invocationDataPublisherConfig(publisherConfig)
            .invocationDataPublisherIntervalSeconds(pp.publishIntervalSeconds)
            .invocationDataPublisherName("http")
            .invocationDataPublisherRetryIntervalSeconds(pp.retryIntervalSeconds)
            .build()
    }

    @Override
    @Transactional(readOnly = true)
    override fun savePublication(
        publicationType: PublicationType,
        licenseKey: String,
        codebaseFingerprint: String,
        publicationSize: Int,
        inputStream: InputStream
    ): File {
        TODO("Not yet implemented")
    }

    override fun generatePublicationFile(
        publicationType: PublicationType,
        customerId: Long,
        correlationId: String
    ): File = File(
        settings.fileImportQueuePath,
        String.format("%s-%d-%s.ser", publicationType, customerId, correlationId)
    )

    override fun getPublicationTypeFromPublicationFile(publicationFile: File): Optional<PublicationType> {
        val fileName = publicationFile.name
        val matcher = CORRELATION_ID_PATTERN.matcher(fileName)
        if (matcher.matches()) {
            return Optional.of(
                PublicationType.valueOf(
                    matcher.group(1).toUpperCase()
                )
            )
        }
        logger.warn("Could not parse publicationType from publication file name {}", fileName)
        return Optional.empty()
    }

    override fun getCorrelationIdFromPublicationFile(publicationFile: File): String {
        val fileName = publicationFile.name
        val matcher = CORRELATION_ID_PATTERN.matcher(fileName)
        if (matcher.matches()) {
            return matcher.group(3)
        }
        logger.warn(
            "Could not parse correlationId from publication file name {}, generating a new...",
            fileName
        )
        return CorrelationIdHolder.generateNew()
    }
}