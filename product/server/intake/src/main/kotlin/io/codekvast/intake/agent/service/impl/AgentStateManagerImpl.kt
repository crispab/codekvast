/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
package io.codekvast.intake.agent.service.impl

import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.messaging.EventService
import io.codekvast.common.messaging.model.AgentPolledEvent
import io.codekvast.common.thread.NamedThreadTemplate
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import io.codekvast.intake.metrics.IntakeMetricsService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** @author olle.hallin@crisp.se
 */
@Component
class AgentStateManagerImpl(
    private val settings: CodekvastIntakeSettings,
    private val customerService: CustomerService,
    private val eventService: EventService,
    private val intakeDAO: IntakeDAO,
    private val intakeMetricsService: IntakeMetricsService
) : AgentStateManager {
    val logger by LoggerDelegate()

    @Scheduled(
        initialDelayString = "\${codekvast.agent-statistics.delay.seconds:60}000",
        fixedRateString = "\${codekvast.agent-statistics.interval.seconds:60}000"
    )
    @Transactional(readOnly = true)
    fun countAgents() {
        NamedThreadTemplate().doInNamedThread("metrics") {
            val statistics =
                intakeDAO.getAgentStatistics(Instant.now().minusSeconds(10))
            logger.debug("Collected {}", statistics)
            intakeMetricsService.gaugeAgents(statistics)
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun updateAgentState(
        customerData: CustomerData, jvmUuid: String, appName: String, environment: String
    ): Boolean {
        intakeDAO.writeLockAgentStateForCustomer(customerData.customerId)
        return doUpdateAgentState(customerData, jvmUuid, appName, environment)
    }

    private fun doUpdateAgentState(
        customerData: CustomerData, jvmUuid: String, appName: String, environment: String
    ): Boolean {
        val customerId = customerData.customerId
        val now = Instant.now()

        intakeDAO.markDeadAgentsAsGarbage(
            customerId, jvmUuid, now.minusSeconds(settings.fileImportIntervalSeconds * 2L)
        )

        intakeDAO.setAgentTimestamps(
            customerId,
            jvmUuid,
            now,
            now.plusSeconds(customerData.pricePlan.pollIntervalSeconds.toLong())
        )
        val cd = customerService.registerAgentPoll(customerData, now)
        val numOtherEnabledAliveAgents: Int =
            intakeDAO.getNumOtherEnabledAliveAgents(customerId, jvmUuid, now.minusSeconds(10))
        val event = AgentPolledEvent.builder()
            .afterTrialPeriod(cd.isTrialPeriodExpired(now))
            .appName(appName)
            .customerId(customerId)
            .disabledEnvironment(!intakeDAO.isEnvironmentEnabled(customerId, jvmUuid))
            .environment(environment)
            .jvmUuid(jvmUuid)
            .polledAt(now)
            .tooManyLiveAgents(
                numOtherEnabledAliveAgents >= customerData.pricePlan.maxNumberOfAgents
            )
            .trialPeriodEndsAt(cd.trialPeriodEndsAt)
            .build()
        logger.debug(
            "Agent {} is {}",
            jvmUuid,
            if (event.isAgentEnabled) "enabled" else "disabled"
        )
        eventService.send(event)
        intakeDAO.updateAgentEnabledState(customerId, jvmUuid, event.isAgentEnabled)
        return event.isAgentEnabled
    }
}