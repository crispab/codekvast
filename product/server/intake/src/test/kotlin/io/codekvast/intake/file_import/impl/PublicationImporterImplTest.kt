package io.codekvast.intake.file_import.impl

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockManager
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.lock.LockTimeoutException
import io.codekvast.common.messaging.CorrelationIdHolder
import io.codekvast.intake.agent.service.AgentService
import io.codekvast.intake.file_import.CodeBaseImporter
import io.codekvast.intake.file_import.InvocationDataImporter
import io.codekvast.intake.file_import.PublicationImporter
import org.assertj.core.util.Files
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.springframework.dao.DuplicateKeyException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.validation.Validator
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** @author olle.hallin@crisp.se
 */
internal class PublicationImporterImplTest {
    @Mock
    private lateinit var codeBaseImporter: CodeBaseImporter

    @Mock
    private lateinit var invocationDataImporter: InvocationDataImporter

    @Mock
    private lateinit var validator: Validator

    @Mock
    private lateinit var agentService: AgentService

    @Mock
    private lateinit var lockManager: LockManager

    private lateinit var publicationImporter: PublicationImporter

    @BeforeEach
    fun beforeTest() {
        MockitoAnnotations.openMocks(this)
        whenever(agentService.getCorrelationIdFromPublicationFile(any()))
            .thenReturn(CorrelationIdHolder.generateNew())
        whenever(lockManager.acquireLock(any()))
            .thenReturn(
                Optional.of(
                    Lock.forPublication(
                        File("/intake/queue/invocations-29-683ce793-8bb8-4dc9-a494-cb2543aa7964.ser")
                    )
                )
            )

        publicationImporter = PublicationImporterImpl(
            codeBaseImporter,
            invocationDataImporter,
            validator,
            agentService,
            LockTemplate(lockManager)
        )
    }

    @Test
    fun should_import_CodeBasePublication3() {
        // given
        val file = getResourceAsFile("/sample-publications/codebase-v2.ser")
        whenever(codeBaseImporter.importPublication(any()))
            .thenReturn(true)

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertTrue(handled)

        verify(codeBaseImporter).importPublication(any())
        verify(validator).validate(any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_retry_CodeBasePublication3_when_DuplicateKeyException() {
        // given
        val file = getResourceAsFile("/sample-publications/codebase-v2.ser")
        whenever(codeBaseImporter.importPublication(any()))
            .thenThrow(DuplicateKeyException("Thrown by mock"))

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertFalse(handled)
        verify(codeBaseImporter).importPublication(any())
        verify(validator).validate(any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_swallow_CodeBasePublication2_when_InvalidClassException() {
        // given
        val file = getResourceAsFile("/sample-publications/codebase-v2-bad-serialVersionUID.ser")

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertTrue(handled)
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_import_InvocationDataPublication2() {
        // given
        val file = getResourceAsFile("/sample-publications/invocations-v2.ser")
        whenever(invocationDataImporter.importPublication(any()))
            .thenReturn(true)

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertTrue(handled)
        verify(invocationDataImporter).importPublication(any())
        verify(validator).validate(ArgumentMatchers.any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_not_process_locked_file() {
        // given
        val file = getResourceAsFile("/sample-publications/invocations-v2.ser")
        whenever(lockManager.acquireLock(any())).thenReturn(Optional.empty())

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertFalse(handled)
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_retry_InvocationDataPublication2_when_DuplicateKeyException() {
        // given
        val file = getResourceAsFile("/sample-publications/invocations-v2.ser")
        whenever(invocationDataImporter.importPublication(any()))
            .thenThrow(DuplicateKeyException("Thrown by mock"))

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertFalse(handled)
        verify(invocationDataImporter).importPublication(any())
        verify(validator).validate(any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_retry_InvocationDataPublication2_when_LockTimeoutException() {
        // given
        val file = getResourceAsFile("/sample-publications/invocations-v2.ser")
        whenever(invocationDataImporter.importPublication(any()))
            .thenThrow(LockTimeoutException("Thrown by mock"))

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertFalse(handled)
        verify(invocationDataImporter).importPublication(any())
        verify(validator).validate(ArgumentMatchers.any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_ignore_unrecognized_content() {
        // given
        val file = Files.newTemporaryFile()
        file.deleteOnExit()
        ObjectOutputStream(BufferedOutputStream(FileOutputStream(file))).use { oos ->
            oos.writeObject("Hello, World!")
        }

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertFalse(handled)
        verify(validator).validate(ArgumentMatchers.anyString())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    @Test
    fun should_handle_invalid_content() {
        // given
        val file = getResourceAsFile("/sample-publications/invocations-v2.ser")
        whenever(validator.validate(any<Any>()))
            .thenReturn(setOf(mock()))

        // when
        val handled = publicationImporter.importPublicationFile(file)

        // then
        assertTrue(handled)
        verify(validator).validate(ArgumentMatchers.any<Any>())
        verifyNoMoreInteractions(codeBaseImporter, invocationDataImporter, validator)
    }

    private fun getResourceAsFile(path: String) = File(javaClass.getResource(path).toURI())
}
