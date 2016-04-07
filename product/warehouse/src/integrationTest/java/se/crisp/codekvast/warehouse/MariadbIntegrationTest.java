package se.crisp.codekvast.warehouse;

import org.flywaydb.core.Flyway;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.testsupport.docker.DockerContainer;
import se.crisp.codekvast.testsupport.docker.MariaDbContainerReadyChecker;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
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
            .build();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Flyway flyway;

    @Test
    public void should_apply_flyway_migrations_on_empty_database() throws Exception {
        // given

        // when

        // then
        assertThat(flyway.info().applied().length, greaterThan(0));
        assertThat(flyway.info().pending().length, is(0));

        assertThat(countRowsInTable("import_file_info"), is(0));
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
