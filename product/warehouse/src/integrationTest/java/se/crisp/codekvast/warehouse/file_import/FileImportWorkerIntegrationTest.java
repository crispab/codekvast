package se.crisp.codekvast.warehouse.file_import;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import se.crisp.codekvast.warehouse.CodekvastWarehouse;

import javax.inject.Inject;
import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodekvastWarehouse.class)
@IntegrationTest
@ActiveProfiles("h2")
public class FileImportWorkerIntegrationTest {

    @Inject
    private FileImportWorker worker;

    @Inject
    private JdbcTemplate jdbcTemplate;
    @Test
    public void shouldImport_V1_ZipFile() throws Exception {
        File zipFile = new File(getClass().getResource("/file_import/sample-ltw-v1.zip").toURI());
        worker.importZipFile(zipFile);

        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "applications"), is(1));
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "import_file_info"), is(1));
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "invocations"), is(11));
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "jvms"), is(2));
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "methods"), is(11));
    }
}
