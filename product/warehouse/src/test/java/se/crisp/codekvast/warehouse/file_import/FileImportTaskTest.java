package se.crisp.codekvast.warehouse.file_import;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.warehouse.config.CodekvastSettings;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(MockitoJUnitRunner.class)
public class FileImportTaskTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private ZipFileImporter importer;

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
    public void should_delete_file_after_import() throws Exception {
        // given
        settings.setDeleteImportedFiles(true);
        File file = createImportFile(".zip");

        // when
        task.importDaemonFiles();

        // then
        verify(importer).importZipFile(file);
        assertThat(file.exists(), is(false));
    }

    @Test
    public void should_not_delete_file_after_import() throws Exception {
        // given
        settings.setDeleteImportedFiles(false);
        File file = createImportFile(".zip");

        // when
        task.importDaemonFiles();

        // then
        verify(importer).importZipFile(file);
        assertThat(file.exists(), is(true));
    }

    private File createImportFile(String suffix) throws IOException {
        return File.createTempFile("import", suffix, temporaryFolder.newFolder());
    }

}
