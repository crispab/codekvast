package io.codekvast.intake.agent.service.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.customer.PricePlan
import io.codekvast.common.customer.PricePlanDefaults
import io.codekvast.common.messaging.EventService
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import io.codekvast.intake.metrics.IntakeMetricsService
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.*

class AgentStateManagerImplTest {
    private val customerId = 1L
    private val jvmUuid = "jvmUuid"
    private val appName = "appName"
    private val environment = "environment"

    @Mock
    private lateinit var intakeDAO: IntakeDAO

    @Mock
    private lateinit var customerService: CustomerService

    @Mock
    private lateinit var eventService: EventService

    @Mock
    private lateinit var intakeMetricsService: IntakeMetricsService
    private lateinit var settings: CodekvastIntakeSettings
    private lateinit var customerData: CustomerData
    private lateinit var agentStateManager: AgentStateManager

    @BeforeEach
    fun beforeTest(@TempDir temporaryFolder: File) {
        MockitoAnnotations.openMocks(this)

        settings = CodekvastIntakeSettings(
            fileImportQueuePath = temporaryFolder,
            fileImportIntervalSeconds = 60
        )

        agentStateManager = AgentStateManagerImpl(
            settings, customerService, eventService, intakeDAO, intakeMetricsService
        )

        setupCustomerData(null, null)
    }

    @Test
    fun should_return_enabled_publishers_when_failed_to_acquire_lock() {
        // given
        whenever(
            intakeDAO.getNumOtherEnabledAliveAgents(
                eq(customerId),
                eq(jvmUuid),
                any()
            )
        ).thenReturn(1)
        whenever(
            intakeDAO.isEnvironmentEnabled(
                eq(customerId),
                eq(jvmUuid)
            )
        ).thenReturn(true)

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)
        assertThat(response, `is`(true))
    }

    @Test
    fun should_return_enabled_publishers_when_below_agent_limit_no_trial_period() {
        // given
        whenever(
            intakeDAO.getNumOtherEnabledAliveAgents(
                eq(customerId),
                eq(jvmUuid),
                any()
            )
        ).thenReturn(1)
        whenever(
            intakeDAO.isEnvironmentEnabled(
                eq(customerId),
                eq(jvmUuid)
            )
        ).thenReturn(true)

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)

        // then
        assertThat(response, `is`(true))
        verify(intakeDAO).updateAgentEnabledState(customerId, jvmUuid, true)
        verify(eventService).send(any())
    }

    @Test
    fun should_return_enabled_publishers_when_below_agent_limit_within_trial_period() {
        // given
        val now = Instant.now()
        setupCustomerData(now.minus(10, DAYS), now.plus(10, DAYS))
        whenever(
            intakeDAO.getNumOtherEnabledAliveAgents(
                eq(customerId),
                eq(jvmUuid),
                any()
            )
        ).thenReturn(1)
        whenever(
            intakeDAO.isEnvironmentEnabled(
                eq(customerId),
                eq(jvmUuid)
            )
        ).thenReturn(true)

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)

        // then
        assertThat(response, `is`(true))
        verify(eventService).send(any())
    }

    @Test
    fun should_return_disabled_publishers_when_below_agent_limit_after_trial_period_has_expired() {
        // given
        val now: Instant = Instant.now()
        setupCustomerData(now.minus(10, DAYS), now.minus(1, DAYS))
        whenever(
            intakeDAO.getNumOtherEnabledAliveAgents(
                eq(customerId),
                eq(jvmUuid),
                any()
            )
        ).thenReturn(1)
        whenever(
            intakeDAO.isEnvironmentEnabled(
                eq(customerId),
                eq(jvmUuid)
            )
        ).thenReturn(true)

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)

        // then
        assertThat(response, `is`(false))
        verify(eventService).send(any())
    }

    @Test
    fun should_return_disabled_publishers_when_above_agent_limit_no_trial_period() {
        // given
        whenever(
            intakeDAO.getNumOtherEnabledAliveAgents(
                eq(customerId),
                eq(jvmUuid),
                any()
            )
        ).thenReturn(10)
        whenever(
            intakeDAO.isEnvironmentEnabled(
                eq(customerId),
                eq(jvmUuid)
            )
        ).thenReturn(true)

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)

        // then
        assertThat(response, `is`(false))
        verify(eventService).send(any())
    }

    @Test
    fun should_return_disabled_publishers_when_below_agent_limit_disabled_environment() {
        // given
        whenever(intakeDAO.getNumOtherEnabledAliveAgents(eq(customerId), eq(jvmUuid), any()))
            .thenReturn(1)

        whenever(intakeDAO.isEnvironmentEnabled(eq(customerId), eq(jvmUuid)))
            .thenReturn(false)

        whenever(intakeDAO.getEnvironmentName(eq(jvmUuid))).thenReturn(Optional.of("environment"))

        // when
        val response =
            agentStateManager.updateAgentState(customerData, jvmUuid, appName, environment)

        // then
        assertThat(response, `is`(false))
        verify(eventService).send(any())
    }

    private fun setupCustomerData(collectionStartedAt: Instant?, trialPeriodEndsAt: Instant?) {
        customerData = CustomerData.builder()
            .customerId(customerId)
            .customerName("name")
            .source("source")
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST))
            .collectionStartedAt(collectionStartedAt)
            .trialPeriodEndsAt(trialPeriodEndsAt)
            .build()
        whenever(customerService.getCustomerDataByLicenseKey(any())).thenReturn(customerData)
        whenever(customerService.registerAgentPoll(any(), any())).thenReturn(customerData)
    }
}