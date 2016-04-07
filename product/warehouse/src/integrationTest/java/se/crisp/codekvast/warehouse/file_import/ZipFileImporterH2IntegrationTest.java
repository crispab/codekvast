package se.crisp.codekvast.warehouse.file_import;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.warehouse.CodekvastWarehouse;

import javax.inject.Inject;
import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodekvastWarehouse.class)
@IntegrationTest
@ActiveProfiles({"integrationTest", "h2"})
@Transactional
public class ZipFileImporterH2IntegrationTest {

    private static final File ZIP_FILE1 = getZipFile("/file_import/sample-ltw-v1-1.zip");
    private static final File ZIP_FILE2 = getZipFile("/file_import/sample-ltw-v1-2.zip");
    private static final File ZIP_FILE3 = getZipFile("/file_import/sample-ltw-v1-3.zip");
    private static final File ZIP_FILE4 = getZipFile("/file_import/sample-jenkins-v1-1.zip");
    private static final File ZIP_FILE5 = getZipFile("/file_import/sample-jenkins-v1-2.zip");

    @Inject
    private ZipFileImporter importer;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void should_handle_importing_same_zipFile_twice() throws Exception {
        // given

        // when
        importer.importZipFile(ZIP_FILE1);

        // then
        assertThat(countRowsInTable("import_file_info"), is(1));
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(countRowsInTable("invocations"), is(11));
        assertThat(countRowsInTable("jvms"), is(2));
        assertThat(countRowsInTable("methods"), is(11));

        // when
        importer.importZipFile(ZIP_FILE1);

        // then
        assertThat(countRowsInTable("import_file_info"), is(1));
    }

    @Test
    public void should_handle_importing_multiple_zips_from_same_app() throws Exception {
        // given

        // when
        importer.importZipFile(ZIP_FILE1);
        importer.importZipFile(ZIP_FILE2);
        importer.importZipFile(ZIP_FILE3);

        // then
        assertThat(countRowsInTable("import_file_info"), is(3));
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(countRowsInTable("invocations"), is(33));
        assertThat(countRowsInTable("jvms"), is(4));
        assertThat(countRowsInTable("methods"), is(11));
    }

    @Test
    @Ignore
    public void should_handle_importing_multiple_dumps_from_same_app() throws Exception {
        // given

        // when
        importer.importZipFile(ZIP_FILE4);
        importer.importZipFile(ZIP_FILE5);

        // then
        assertThat(countRowsInTable("import_file_info"), is(2));
        assertThat(countRowsInTable("applications"), is(3));
        assertThat(countRowsInTable("invocations"), is(57725));
        assertThat(countRowsInTable("jvms"), is(3));
        assertThat(countRowsInTable("methods"), is(11));
    }

    private static File getZipFile(String name) {
        try {
            return new File(ZipFileImporterH2IntegrationTest.class.getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
