package se.crisp.codekvast.warehouse.file_import;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
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
@IntegrationTest({
        "spring.datasource.url=jdbc:h2:mem:zipFileImporterIntegrationTest",
        "flyway.placeholders.ifH2=",
        "flyway.placeholders.ifMariadb=--",
        "flyway.placeholders.ifMariadbStart=/*",
        "flyway.placeholders.ifMariadbEnd=*/",
        "codekvast.importPathPollIntervalSeconds=82400",
})
@Transactional
public class ZipFileImporterIntegrationTest {

    @Inject
    private ZipFileImporter importer;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void should_handle_importing_same_zipFile_twice() throws Exception {
        // given
        assertThat(countRowsInTable("import_file_info"), is(0));
        File zipFile = getZipFile("/file_import/sample-ltw-v1-1.zip");

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
        assertThat(countRowsInTable("import_file_info"), is(0));

        // when
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-1.zip"));
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-2.zip"));

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
