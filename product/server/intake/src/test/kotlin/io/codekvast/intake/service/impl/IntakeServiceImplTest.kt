package io.codekvast.intake.service.impl

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.customer.LicenseViolationException
import io.codekvast.common.messaging.CorrelationIdHolder
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.model.PublicationType.*
import io.codekvast.intake.service.IntakeService
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.util.*

internal class IntakeServiceImplTest {
    @TempDir
    private lateinit var temporaryFolder: File

    @Mock
    private lateinit var customerService: CustomerService

    @Mock
    private lateinit var intakeDAO: IntakeDAO

    @Mock
    private lateinit var intakeMetricsService: IntakeMetricsService

    private val customerData = CustomerData.sample()
    private lateinit var service: IntakeService

    @BeforeEach
    fun beforeTest() {
        MockitoAnnotations.openMocks(this)
        val settings = CodekvastIntakeSettings(fileImportQueuePath = temporaryFolder)

        whenever(customerService.getCustomerDataByLicenseKey(ArgumentMatchers.anyString()))
            .thenReturn(customerData)

        service = IntakeServiceImpl(
            settings,
            customerService,
            intakeDAO,
            mock(),
            intakeMetricsService
        )
    }

    @Test
    fun should_close_inputStream_after_throwing() {
        // given
        val publicationSize = 4711

        Mockito.doThrow(LicenseViolationException("stub"))
            .`when`(customerService)
            .assertPublicationSize(
                ArgumentMatchers.any(CustomerData::class.java),
                ArgumentMatchers.eq(publicationSize)
            )
        val inputStream = Mockito.mock(InputStream::class.java)
        try {
            // when
            service.savePublication(
                CODEBASE,
                "key",
                "fingerprint",
                publicationSize,
                inputStream
            )

            // then
            fail<Any>("Expected a LicenseViolationException")
        } catch (expected: LicenseViolationException) {
            // Expected outcome
        } finally {
            Mockito.verify(inputStream).close()
        }
    }

    @Test
    @Throws(Exception::class)
    fun should_close_inputStream_after_not_throwing() {
        // given
        val inputStream = Mockito.mock(InputStream::class.java)

        // when
        service.savePublication(
            CODEBASE,
            "key",
            "fingerprint",
            4711,
            inputStream
        )

        // then
        Mockito.verify(inputStream).close()
    }

    @Test
    @Throws(Exception::class)
    fun should_save_uploaded_codebase() {
        // given
        val contents = "Dummy Code Base Publication"

        // when
        val resultingFile: File = service.savePublication(
            CODEBASE,
            "key",
            "fingerprint",
            1000,
            ByteArrayInputStream(contents.toByteArray())
        )

        // then
        MatcherAssert.assertThat(resultingFile, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(
            service.getPublicationTypeFromPublicationFile(resultingFile),
            `is`(Optional.of(CODEBASE))
        )
        MatcherAssert.assertThat(resultingFile.name, CoreMatchers.startsWith("codebase-"))
        MatcherAssert.assertThat(resultingFile.name, CoreMatchers.endsWith(".ser"))
        MatcherAssert.assertThat(resultingFile.exists(), `is`(true))
        MatcherAssert.assertThat(
            resultingFile.length(), `is`(
                contents.length.toLong()
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_save_uploaded_invocations() {
        // given
        val contents = "Dummy Invocations Publication"

        // when
        val resultingFile: File = service.savePublication(
            INVOCATIONS,
            "key",
            "fingerprint",
            1000,
            ByteArrayInputStream(contents.toByteArray())
        )

        // then
        MatcherAssert.assertThat(resultingFile, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(resultingFile.name, CoreMatchers.startsWith("invocations-"))
        MatcherAssert.assertThat(resultingFile.name, CoreMatchers.endsWith(".ser"))
        MatcherAssert.assertThat(resultingFile.exists(), `is`(true))
        MatcherAssert.assertThat(
            resultingFile.length(), `is`(
                contents.length.toLong()
            )
        )
    }

    @Test
    fun should_build_file_name_with_correlationId() {
        // given
        val correlationId = CorrelationIdHolder.generateNew()

        // when
        val file: File = service.generatePublicationFile(
            CODEBASE,
            17L,
            correlationId
        )

        // then
        MatcherAssert.assertThat(file.name, CoreMatchers.containsString(correlationId))

        // when
        val correlationId2: String = service.getCorrelationIdFromPublicationFile(file)

        // then
        MatcherAssert.assertThat(correlationId2, `is`(correlationId))
    }

    @Test
    fun should_count_agent_polls() {

        // when
        service.getConfig1(GetConfigRequest1.sample())

        // then
        Mockito.verify(intakeMetricsService, Mockito.times(1))
            .countAgentPoll()
    }
}