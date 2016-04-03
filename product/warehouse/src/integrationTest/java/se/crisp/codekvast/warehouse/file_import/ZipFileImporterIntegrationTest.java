package se.crisp.codekvast.warehouse.file_import;

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
@ActiveProfiles("integrationTest")
@Transactional
public class ZipFileImporterIntegrationTest {

    private static final String ZIP_FILE1 = "/file_import/sample-ltw-v1-1.zip";
    private static final String ZIP_FILE2 = "/file_import/sample-ltw-v1-2.zip";

    @Inject
    private ZipFileImporter importer;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void should_handle_importing_same_zipFile_twice() throws Exception {
        // given
        File zipFile = getZipFile(ZIP_FILE1);

        // when
        importer.importZipFile(zipFile);

        // then
        assertThat(countRowsInTable("import_file_info"), is(1));
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(countRowsInTable("invocations"), is(11));
        assertThat(countRowsInTable("jvms"), is(2));
        assertThat(countRowsInTable("methods"), is(11));

        // when
        importer.importZipFile(zipFile);

        // then
        assertThat(countRowsInTable("import_file_info"), is(1));
    }

    @Test
    public void should_handle_importing_two_zips_from_same_app() throws Exception {
        // given

        // when
        importer.importZipFile(getZipFile(ZIP_FILE1));
        importer.importZipFile(getZipFile(ZIP_FILE2));

        // then
        assertThat(countRowsInTable("import_file_info"), is(2));
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(countRowsInTable("invocations"), is(11));
        assertThat(countRowsInTable("jvms"), is(2));
        assertThat(countRowsInTable("methods"), is(11));
    }

    private File getZipFile(String name) throws URISyntaxException {
        return new File(getClass().getResource(name).toURI());
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
