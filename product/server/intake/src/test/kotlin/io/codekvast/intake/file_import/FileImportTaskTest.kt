package io.codekvast.intake.file_import

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockManager
import io.codekvast.common.lock.LockTemplate
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.hamcrest.Matchers.`is` as is_

/** @author olle.hallin@crisp.se
 */
class FileImportTaskTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Mock
    private lateinit var importer: PublicationImporter

    @Mock
    private lateinit var lockManager: LockManager

    @BeforeEach
    fun beforeTest() {
        MockitoAnnotations.openMocks(this)
        whenever(lockManager.acquireLock(ArgumentMatchers.any()))
            .thenReturn(Optional.of(Lock.forTask("test", 60)))
    }

    private fun createFileImportTask(deleteImportedFiles: Boolean): FileImportTask {
        val settings = CodekvastIntakeSettings(
            fileImportQueuePath = temporaryFolder,
            deleteImportedFiles = deleteImportedFiles
        )
        return FileImportTask(settings, importer, mock(), LockTemplate(lockManager))
    }

    @Test
    fun should_handle_empty_fileImportQueuePath() {
        // given
        // An empty fileImportQueuePath
        val task = createFileImportTask(true)

        // when
        task.importPublicationFiles()

        // then
        verifyNoMoreInteractions(importer)
    }

    @Test
    fun should_ignore_non_ser_files() {
        // given
        val file = createImportFile(".bar")
        val task = createFileImportTask(true)

        // when
        task.importPublicationFiles()

        // then
        assertTrue(file.exists())
        verifyNoMoreInteractions(importer)
    }

    @Test
    fun should_delete_file_after_successful_import() {
        // given
        whenever(importer.importPublicationFile(any())).thenReturn(true)
        val file = createImportFile(".ser")
        val task = createFileImportTask(true)

        // when
        task.importPublicationFiles()

        // then
        verify(importer).importPublicationFile(file)
        assertFalse(file.exists())
    }

    @Test
    fun should_not_delete_file_after_failed_import() {
        // given
        whenever(importer.importPublicationFile(any())).thenReturn(false)
        val file = createImportFile(".ser")
        val task = createFileImportTask(true)

        // when
        task.importPublicationFiles()

        // then
        verify(importer).importPublicationFile(file)
        assertTrue(file.exists())
    }

    @Test
    fun should_not_delete_file_after_import() {
        // given
        val file = createImportFile(".ser")
        val task = createFileImportTask(false)

        // when
        task.importPublicationFiles()

        // then
        verify(importer).importPublicationFile(file)
        assertTrue(file.exists())
    }

    private fun createImportFile(suffix: String): File {
        return File.createTempFile("import-", suffix, temporaryFolder)
    }
}