package io.codekvast.dashboard.file_import;

import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class FileImportTaskTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PublicationImporter importer;

    private CodekvastDashboardSettings settings;

    private FileImportTask task;

    @Before
    public void beforeTest() {
        settings = new CodekvastDashboardSettings();
        settings.setQueuePath(temporaryFolder.getRoot());

        task = new FileImportTask(settings, importer);
    }

    @Test
    public void should_handle_empty_queuePath() {
        // given
        // An empty queuePath

        // when
        task.importPublicationFiles();

        // then
        verifyNoMoreInteractions(importer);
    }

    @Test
    public void should_ignore_non_zip_files() throws Exception {
        // given
        File file = createImportFile(".bar");

        // when
        task.importPublicationFiles();

        // then
        assertThat(file.exists(), is(true));
        verifyNoMoreInteractions(importer);
    }

    @Test
    public void should_delete_file_after_successful_import() throws Exception {
        // given
        settings.setDeleteImportedFiles(true);
        File file = createImportFile(".ser");
        when(importer.importPublicationFile(any(File.class))).thenReturn(true);

        // when
        task.importPublicationFiles();

        // then
        verify(importer).importPublicationFile(file);
        assertThat(file.exists(), is(false));
    }

    @Test
    public void should_not_delete_file_after_failed_import() throws Exception {
        // given
        settings.setDeleteImportedFiles(true);
        File file = createImportFile(".ser");
        when(importer.importPublicationFile(any(File.class))).thenReturn(false);

        // when
        task.importPublicationFiles();

        // then
        verify(importer).importPublicationFile(file);
        assertThat(file.exists(), is(true));
    }

    @Test
    public void should_not_delete_file_after_import() throws Exception {
        // given
        settings.setDeleteImportedFiles(false);
        File file = createImportFile(".ser");

        // when
        task.importPublicationFiles();

        // then
        verify(importer).importPublicationFile(file);
        assertThat(file.exists(), is(true));
    }

    private File createImportFile(String suffix) throws IOException {
        return File.createTempFile("import", suffix, temporaryFolder.newFolder());
    }

}
