package se.crisp.codekvast.warehouse;

import org.flywaydb.core.Flyway;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.testsupport.docker.DockerContainer;
import se.crisp.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import se.crisp.codekvast.warehouse.file_import.ZipFileImporter;

import javax.inject.Inject;
import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
@SpringApplicationConfiguration(classes = CodekvastWarehouse.class)
@IntegrationTest
@ActiveProfiles({"integrationTest"})
@Transactional
public class MariadbIntegrationTest {

    private static final File ZIP_FILE1 = getZipFile("/file_import/sample-ltw-v1-1.zip");
    private static final File ZIP_FILE2 = getZipFile("/file_import/sample-ltw-v1-2.zip");
    private static final File ZIP_FILE3 = getZipFile("/file_import/sample-ltw-v1-3.zip");

    @ClassRule
    public static DockerContainer mariadb = DockerContainer
            .builder()
            .imageName("mariadb:10")
            .port("3306")

            .env("MYSQL_ROOT_PASSWORD=root")
            .env("MYSQL_DATABASE=codekvast_warehouse")
            .env("MYSQL_USER=codekvast")
            .env("MYSQL_PASSWORD=codekvast")

            .readyChecker(
                    MariaDbContainerReadyChecker.builder()
                                                .host("localhost")
                                                .internalPort(3306)
                                                .database("codekvast_warehouse")
                                                .username("codekvast")
                                                .password("codekvast")
                                                .timeoutSeconds(120)
                                                .assignJdbcUrlToSystemProperty("spring.datasource.url")
                                                .build())
            .leaveContainerRunning(true)
            .build();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Flyway flyway;

    @Inject
    private ZipFileImporter importer;

    @Test
    public void should_apply_all_flyway_migrations_on_empty_database() throws Exception {
        // given

        // when

        // then
        assertThat(flyway.info().applied().length, is(9));
        assertThat(flyway.info().pending().length, is(0));
    }

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
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_status_enum_correctly() throws Exception {
        int methodId = 0;
        long now = System.currentTimeMillis();
        for (SignatureStatus status : SignatureStatus.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, " +
                                        "invocationCount, status) VALUES(11, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

    }

    private static File getZipFile(String name) {
        try {
            return new File(MariadbIntegrationTest.class.getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
