package integrationTest.warehouse;

import integrationTest.warehouse.testdata.TestDataGenerator;
import io.codekvast.javaagent.model.v1.SignatureStatus;
import io.codekvast.testsupport.docker.DockerContainer;
import io.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import io.codekvast.warehouse.CodekvastWarehouse;
import io.codekvast.warehouse.webapp.WebappService;
import io.codekvast.warehouse.webapp.model.GetMethodsRequest1;
import io.codekvast.warehouse.webapp.model.MethodDescriptor1;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
@SpringBootTest(
        classes = {CodekvastWarehouse.class, TestDataGenerator.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
@Transactional(rollbackFor = Exception.class)
public class MariadbIntegrationTest {

    private final long now = System.currentTimeMillis();

    private static final int PORT = 3306;
    private static final String DATABASE = "codekvast";
    private static final String USERNAME = "codekvast";
    private static final String PASSWORD = "codekvast";

    @ClassRule
    public static DockerContainer mariadb = DockerContainer
            .builder()
            .imageName("mariadb:10")
            .port("" + PORT)

            .env("MYSQL_ROOT_PASSWORD=root")
            .env("MYSQL_DATABASE=" + DATABASE)
            .env("MYSQL_USER=" + USERNAME)
            .env("MYSQL_PASSWORD=" + PASSWORD)

            .readyChecker(
                    MariaDbContainerReadyChecker.builder()
                                                .host("localhost")
                                                .internalPort(PORT)
                                                .database(DATABASE)
                                                .username(USERNAME)
                                                .password(PASSWORD)
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


    @Inject
    private WebappService webappService;

    @Inject
    private TestDataGenerator testDataGenerator;

    @Before
    public void beforeTest() throws Exception {
        assumeTrue(mariadb.isRunning());
    }

    @Test
    public void should_have_applied_all_flyway_migrations_to_an_empty_database() throws Exception {
        // given

        // when

        // then
        assertThat("Wrong number of pending Flyway migrations", flyway.info().pending().length, is(0));
        assertThat(countRowsInTable("schema_version WHERE success != 1"), is(0));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_all_signature_status_enum_values_correctly() throws Exception {
        // given

        // when
        int methodId = 0;
        for (SignatureStatus status : SignatureStatus.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(customerId, applicationId, methodId, jvmId, invokedAtMillis, " +
                                        "invocationCount, status) VALUES(1, 11, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

        // then
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(SignatureStatus.values().length));
    }

    // TODO: add tests for CodeBasePublication import

    // TODO: add tests for InvocationDataPublication import

    @Test
    public void should_query_by_IDEA_signature_correctly() throws Exception {
        // given

        // when

        // then
    }

    @Test
    public void should_query_by_signature_suffix_correctly() throws Exception {
        // given

        // when find substring

        // then
    }

    @Test
    public void should_query_by_signature_not_normalize_but_no_match() throws Exception {
        // given

        // when find by signature

        // then
    }

    @Test
    public void should_query_signatures_and_respect_max_results() throws Exception {
        // given

        // when

        // then
    }

    @Test(expected = ConstraintViolationException.class)
    public void should_throw_when_querying_signature_with_too_short_signature() throws Exception {
        // given

        // when query with too short signature
        webappService.getMethods(GetMethodsRequest1.defaults().toBuilder().signature("").build());
    }

    @Test
    public void should_query_unknown_signature_correctly() throws Exception {
        // given

        // when find exact signature
        List<MethodDescriptor1> methods = webappService.getMethods(
            GetMethodsRequest1.defaults().toBuilder().signature("foobar").build());

        // then
        assertThat(methods, hasSize(0));
    }

    @Test
    public void should_query_by_known_id() throws Exception {
        // given
        // generateQueryTestData();

        // List<Long> validIds = jdbcTemplate.query("SELECT id FROM methods", (rs, rowNum) -> rs.getLong(1));

        // when
        // Optional<MethodDescriptor1> result = webappService.getMethodById(validIds.get(0));

        // then
        // assertThat(result.isPresent(), is(true));
    }

    @Test
    public void should_query_by_unknown_id() throws Exception {
        // given
        // generateQueryTestData();

        // when
        Optional<MethodDescriptor1> result = webappService.getMethodById(-1L);

        // then
        assertThat(result.isPresent(), is(false));
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }
}
