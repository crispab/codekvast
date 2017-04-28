package io.codekvast.warehouse.file_import;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class FileImportTaskTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PublicationImporter importer;

    private CodekvastSettings settings;

    private FileImportTask task;

    @Before
    public void beforeTest() throws Exception {
        settings = new CodekvastSettings();
        settings.setImportPath(temporaryFolder.getRoot());

        task = new FileImportTask(settings, importer);
    }

    @Test
    public void should_handle_empty_importPath() throws Exception {
        // given
        // An empty importPath

        // when
        task.importDaemonFiles();

        // then
        verifyNoMoreInteractions(importer);
    }

    @Test
    public void should_ignore_non_zip_files() throws Exception {
        // given
        File file = createImportFile(".bar");

        // when
        task.importDaemonFiles();

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
        task.importDaemonFiles();

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
        task.importDaemonFiles();

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
        task.importDaemonFiles();

        // then
        verify(importer).importPublicationFile(file);
        assertThat(file.exists(), is(true));
    }

    private File createImportFile(String suffix) throws IOException {
        return File.createTempFile("import", suffix, temporaryFolder.newFolder());
    }

}
