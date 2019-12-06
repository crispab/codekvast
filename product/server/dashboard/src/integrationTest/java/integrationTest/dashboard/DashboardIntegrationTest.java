package integrationTest.dashboard;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.CustomerService.LoginRequest;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import io.codekvast.dashboard.CodekvastDashboardApplication;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.agent.impl.AgentDAO;
import io.codekvast.dashboard.dashboard.DashboardService;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse2;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import io.codekvast.dashboard.dashboard.model.status.AgentDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.PublicationImporter;
import io.codekvast.dashboard.weeding.WeedingTask;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.*;
import io.codekvast.javaagent.model.v3.CodeBaseEntry3;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import io.codekvast.testsupport.docker.DockerContainer;
import io.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import io.codekvast.testsupport.docker.RabbitmqContainerReadyChecker;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.junit.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "ClassWithTooManyFields"})
@SpringBootTest(
    classes = {CodekvastDashboardApplication.class, DashboardIntegrationTest.LockContentionTestHelper.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
@Transactional
public class DashboardIntegrationTest {

    private final long now = System.currentTimeMillis();

    private static final int PORT = 3306;
    private static final String DATABASE = "codekvast";
    private static final String USERNAME = "codekvast";
    private static final String PASSWORD = "codekvast";

    @ClassRule
    public static DockerContainer mariadb = DockerContainer
        .builder()
        .imageName("mariadb:10.4")
        .port("" + PORT)

        .env("MYSQL_ROOT_PASSWORD=root")
        .env("MYSQL_DATABASE=" + DATABASE)
        .env("MYSQL_USER=" + USERNAME)
        .env("MYSQL_PASSWORD=" + PASSWORD)
        .env("MYSQL_INITDB_SKIP_TZINFO=true")

        .readyChecker(
            MariaDbContainerReadyChecker.builder()
                                        .hostname("localhost")
                                        .internalPort(PORT)
                                        .database(DATABASE)
                                        .username(USERNAME)
                                        .password(PASSWORD)
                                        .timeoutSeconds(300)
                                        .assignJdbcUrlToSystemProperty("spring.datasource.url")
                                        .build())
        .build();

    @ClassRule
    public static DockerContainer rabbitmq = DockerContainer.builder()
                                                            .imageName("rabbitmq:3.8-management-alpine")
                                                            .port("5672")

                                                            .env("RABBITMQ_DEFAULT_VHOST=/")
                                                            .env("RABBITMQ_DEFAULT_USER=admin")
                                                            .env("RABBITMQ_DEFAULT_PASS=secret")

                                                            .readyChecker(
                                                                RabbitmqContainerReadyChecker.builder()
                                                                                             .host("localhost")
                                                                                             .internalPort(5672)
                                                                                             .vhost("/")
                                                                                             .timeoutSeconds(30)
                                                                                             .username("admin")
                                                                                             .password("secret")
                                                                                             .assignRabbitUrlToSystemProperty(
                                                                                                 "spring.rabbitmq.addresses")
                                                                                             .build())
                                                            .build();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @MockBean
    private CommonMetricsService commonMetricsService;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private Flyway flyway;

    @Inject
    private CustomerService customerService;

    @Inject
    private DashboardService dashboardService;

    @Inject
    private AgentService agentService;

    @Inject
    private AgentDAO agentDAO;

    @Inject
    private PublicationImporter publicationImporter;

    @Inject
    private CodeBaseImporter codeBaseImporter;

    @Inject
    private InvocationDataImporter invocationDataImporter;

    @Inject
    private WeedingTask weedingTask;

    @Inject
    private LockManager lockManager;

    @Inject
    private LockContentionTestHelper lockContentionTestHelper;

    @Before
    public void beforeTest() {
        assumeTrue(mariadb.isRunning());
    }

    @After
    public void afterTest() {
        setSecurityContextCustomerId(null);
    }

    @Test
    public void should_have_applied_all_flyway_migrations_to_an_empty_database() {
        // given

        // when

        // then
        assertThat("Wrong number of pending Flyway migrations", flyway.info().pending().length, is(0));
        assertThat(countRowsInTable("flyway_schema_history WHERE success != 1"), is(0));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_all_signature_status_enum_values_correctly() {
        // given

        // when
        int methodId = 0;
        for (SignatureStatus2 status : SignatureStatus2.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(customerId, applicationId, environmentId, methodId, jvmId, invokedAtMillis, " +
                                    "invocationCount, status) VALUES(1, 1, 1, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

        // then
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(SignatureStatus2.values().length));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByCustomerId_1() {
        CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);
        assertThat(customerData.getCustomerId(), is(1L));
        assertThat(customerData.getCustomerName(), is("Demo"));
        assertThat(customerData.getPricePlan().getName(), is("DEMO"));
        assertThat(customerData.getContactEmail(), is("contactEmail1"));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByCustomerId_2() {
        CustomerData customerData = customerService.getCustomerDataByCustomerId(2L);
        assertThat(customerData.getCustomerId(), is(2L));
        assertThat(customerData.getContactEmail(), nullValue());
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_invalid_getCustomerDataByCustomerId() {
        customerService.getCustomerDataByCustomerId(0L);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByLicenseKey() {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey("");
        assertThat(customerData.getCustomerId(), is(1L));
        assertThat(customerData.getCustomerName(), is("Demo"));
        assertThat(customerData.getPricePlan().getName(), is("DEMO"));
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_invalid_getCustomerDataByLicenseKey() {
        customerService.getCustomerDataByLicenseKey("undefined");
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByExternalId_with_pricePlanOverride() {
        CustomerData customerData = customerService.getCustomerDataByExternalId("test", "external-1");
        assertThat(customerData.getCustomerId(), is(1L));
        assertThat(customerData.getCustomerName(), is("Demo"));
        assertThat(customerData.getPricePlan().getName(), is("DEMO"));
        assertThat(customerData.getPricePlan().getOverrideBy(), is("integration test"));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByExternalId_without_pricePlanOverride() {
        CustomerData customerData = customerService.getCustomerDataByExternalId("test", "external-2");
        assertThat(customerData.getCustomerId(), is(2L));
        assertThat(customerData.getPricePlan().getOverrideBy(), nullValue());
        assertThat(customerData.isTrialPeriodExpired(Instant.now()), is(false));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByExternalId_trialPeriodExpired() {
        Instant now = Instant.parse("2017-09-21T16:21:19Z").plus(1, DAYS); // see base-data.sql
        CustomerData customerData = customerService.getCustomerDataByExternalId("test", "external-3");
        assertThat(customerData.getCustomerId(), is(3L));
        assertThat(customerData.isTrialPeriodExpired(now), is(true));
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_invalid_getCustomerDataByExternalId() {
        customerService.getCustomerDataByExternalId("test", "undefined");
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_add_delete_customer() {
        CustomerService.AddCustomerResponse response = customerService.addCustomer(CustomerService.AddCustomerRequest
                                                                                       .builder()
                                                                                       .source("source")
                                                                                       .externalId("externalId")
                                                                                       .name("customerName")
                                                                                       .plan("test")
                                                                                       .build());

        Long count = jdbcTemplate
            .queryForObject("SELECT COUNT(1) FROM customers WHERE source = ? AND externalId = ?", Long.class, "source", "externalId");

        assertThat(count, is(1L));

        assertThat(response.getLicenseKey(), notNullValue());

        customerService.deleteCustomerByExternalId("source", "externalId");

        count = jdbcTemplate
            .queryForObject("SELECT COUNT(1) FROM customers WHERE source= ? AND externalId = ?", Long.class, "source", "externalId");
        assertThat(count, is(0L));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_delete_customer() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers", Long.class);
        assertThat(count, is(3L));

        customerService.deleteCustomerByExternalId("test", "external-1");

        count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers", Long.class);
        assertThat(count, is(2L));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_register_login_twice() {

        // given
        jdbcTemplate.update(" DELETE FROM users");

        // when
        customerService.registerLogin(LoginRequest.builder()
                                                  .customerId(1L)
                                                  .source("source1")
                                                  .email("email")
                                                  .build());

        customerService.registerLogin(LoginRequest.builder()
                                                  .customerId(1L)
                                                  .source("source2")
                                                  .email("email")
                                                  .build());

        // then
        assertThat(countRowsInTable("users"), is(1));
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_publication_invalid_licenseKey() {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey("undefined");
        customerService.assertPublicationSize(customerData, 10);
    }

    @Test(expected = LicenseViolationException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_publication_too_large() {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey("");
        customerService.assertPublicationSize(customerData, 100_000);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_publication() {
        CustomerData customerData = customerService.getCustomerDataByLicenseKey("");
        customerService.assertPublicationSize(customerData, 10);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_assertDatabaseSize() {
        customerService.assertDatabaseSize(1L);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_start_trial_period_at_first_agent_poll() {
        // given
        Instant now = Instant.now();
        jdbcTemplate.update("UPDATE customers SET plan = 'test', collectionStartedAt = NULL, trialPeriodEndsAt = NULL WHERE id = 1");
        CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);

        assertThat(customerData.getPricePlan().getTrialPeriodDays(), is(PricePlanDefaults.TEST.getTrialPeriodDays()));
        assertThat(customerData.getCollectionStartedAt(), is(nullValue()));
        assertThat(customerData.getTrialPeriodEndsAt(), is(nullValue()));
        assertThat(customerData.isTrialPeriodExpired(now), is(false));

        // when
        customerData = customerService.registerAgentPoll(customerData, now);

        // then
        assertThat(customerData.getCollectionStartedAt(), is(now));
        int days = customerData.getPricePlan().getTrialPeriodDays();
        assertThat(customerData.getTrialPeriodEndsAt(), is(now.plus(days, DAYS)));
        assertThat(customerData.isTrialPeriodExpired(now.plus(days - 1, DAYS)), is(false));
        assertThat(customerData.isTrialPeriodExpired(now.plus(days + 1, DAYS)), is(true));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_not_start_trial_period_at_first_agent_poll() {
        // given
        Instant now = Instant.now();
        CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);

        assertThat(customerData.getPricePlan().getTrialPeriodDays(), is(PricePlanDefaults.DEMO.getTrialPeriodDays()));
        assertThat(customerData.getCollectionStartedAt(), is(nullValue()));
        assertThat(customerData.getTrialPeriodEndsAt(), is(nullValue()));
        assertThat(customerData.isTrialPeriodExpired(now), is(false));

        // when
        customerData = customerService.registerAgentPoll(customerData, now);

        // then
        assertThat(customerData.getCollectionStartedAt(), is(now));
        assertThat(customerData.getTrialPeriodEndsAt(), is(nullValue()));
        assertThat(customerData.isTrialPeriodExpired(now), is(false));
    }

    @Test
    public void should_import_publication_file() {
        // given
        CodeBasePublication3 publication = CodeBasePublication3.builder()
                                                               .commonData(CommonPublicationData2.sampleCommonPublicationData())
                                                               .entries(Arrays.asList(CodeBaseEntry3.sampleCodeBaseEntry()))
                                                               .build();
        File file = writeToTempFile(publication);

        // when
        publicationImporter.importPublicationFile(file);

        // then

    }

    @SneakyThrows
    private File writeToTempFile(Object publication) {
        File file = File.createTempFile(getClass().getSimpleName(), ".ser");
        file.deleteOnExit();
        try (val oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(publication);
            oos.flush();
        }
        return file;
    }

    @Test
    public void should_import_codeBasePublication2() {
        // given
        assertThat(countRowsInTable("applications"), is(0));
        assertThat(countRowsInTable("environments"), is(0));
        assertThat(countRowsInTable("jvms"), is(0));
        assertThat(countRowsInTable("methods"), is(0));
        assertThat(countRowsInTable("method_locations"), is(0));
        assertThat(countRowsInTable("invocations"), is(0));

        //@formatter:off
        CodeBasePublication3 publication = CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry3.sampleCodeBaseEntry()))
            .build();
        //@formatter:on

        // when
        codeBaseImporter.importPublication(publication);

        // then
        assertThat(countRowsInTable("applications WHERE name = '" + publication.getCommonData().getAppName() + "'"), is(1));
        assertThat(countRowsInTable("environments WHERE name = '" + publication.getCommonData().getEnvironment() + "'"), is(1));
        assertThat(countRowsInTable("jvms WHERE uuid = '" + publication.getCommonData().getJvmUuid() + "'"), is(1));
        assertThat(countRowsInTable("methods WHERE signature = '" + publication.getEntries().iterator().next().getSignature() + "'"),
                   is(1));
        assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(1));
        assertThat(countRowsInTable("method_locations"), is(1));
    }

    @Test
    public void should_import_codeBasePublication3() {
        // given
        assertThat(countRowsInTable("applications"), is(0));
        assertThat(countRowsInTable("environments"), is(0));
        assertThat(countRowsInTable("jvms"), is(0));
        assertThat(countRowsInTable("methods"), is(0));
        assertThat(countRowsInTable("invocations"), is(0));

        //@formatter:off
        CodeBasePublication3 publication = CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry3.sampleCodeBaseEntry()))
            .build();
        //@formatter:on

        // when
        codeBaseImporter.importPublication(publication);

        // then
        assertThat(countRowsInTable("applications WHERE name = '" + publication.getCommonData().getAppName() + "'"), is(1));
        assertThat(countRowsInTable("environments WHERE name = '" + publication.getCommonData().getEnvironment() + "'"), is(1));
        assertThat(countRowsInTable("jvms WHERE uuid = '" + publication.getCommonData().getJvmUuid() + "'"), is(1));
        assertThat(countRowsInTable("methods WHERE signature = '" + publication.getEntries().iterator().next().getSignature() + "'"),
                   is(1));
        assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(1));
    }

    @Test
    public void should_import_codeBasePublication2_after_invocationDataPublication() {
        // given
        assertThat(countRowsInTable("applications"), is(0));
        assertThat(countRowsInTable("environments"), is(0));
        assertThat(countRowsInTable("jvms"), is(0));
        assertThat(countRowsInTable("methods"), is(0));
        assertThat(countRowsInTable("invocations"), is(0));

        //@formatter:off
        CodeBasePublication3 codeBasePublication = CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry3.sampleCodeBaseEntry()))
            .build();

        long intervalStartedAtMillis = System.currentTimeMillis();

        InvocationDataPublication2 invocationDataPublication = InvocationDataPublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis)
            .invocations(codeBasePublication.getEntries().stream().map(CodeBaseEntry3::getSignature).collect(Collectors.toSet()))
            .build();
        //@formatter:on

        // when
        invocationDataImporter.importPublication(invocationDataPublication);
        codeBaseImporter.importPublication(codeBasePublication);

        // then
        assertThat(countRowsInTable("applications WHERE name = '" + codeBasePublication.getCommonData().getAppName() + "'"), is(1));
        assertThat(countRowsInTable("environments WHERE name = '" + codeBasePublication.getCommonData().getEnvironment() + "'"), is(1));
        assertThat(countRowsInTable("jvms WHERE uuid = '" + codeBasePublication.getCommonData().getJvmUuid() + "'"), is(1));
        assertThat(
            countRowsInTable("methods WHERE signature = '" + codeBasePublication.getEntries().iterator().next().getSignature() + "'"),
            is(1));
        assertThat(countRowsInTable("invocations WHERE invokedAtMillis = " + intervalStartedAtMillis), is(1));
    }

    @Test
    public void should_import_invocationDataPublication() {
        // given
        assertThat(countRowsInTable("applications"), is(0));
        assertThat(countRowsInTable("environments"), is(0));
        assertThat(countRowsInTable("jvms"), is(0));
        assertThat(countRowsInTable("methods"), is(0));
        assertThat(countRowsInTable("invocations"), is(0));

        long intervalStartedAtMillis = System.currentTimeMillis();

        //@formatter:off
        InvocationDataPublication2 publication = InvocationDataPublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis)
            .invocations(Collections.singleton("signature"))
            .build();
        //@formatter:on

        // when
        invocationDataImporter.importPublication(publication);

        // then
        assertThat(countRowsInTable("applications WHERE name = '" + publication.getCommonData().getAppName() + "'"), is(1));
        assertThat(countRowsInTable("environments WHERE name = '" + publication.getCommonData().getEnvironment() + "'"), is(1));
        assertThat(countRowsInTable("jvms WHERE uuid = '" + publication.getCommonData().getJvmUuid() + "'"), is(1));
        assertThat(countRowsInTable("methods WHERE signature = 'signature'"), is(1));
        assertThat(countRowsInTable("invocations WHERE invokedAtMillis = " + intervalStartedAtMillis), is(1));
    }

    @Test
    public void should_query_unknown_signature_correctly() {
        // given
        setSecurityContextCustomerId(1L);

        // when find exact signature
        GetMethodsResponse2 response = dashboardService.getMethods2(
            GetMethodsRequest.defaults().toBuilder().signature("foobar").build());

        // then
        assertThat(response.getMethods(), hasSize(0));
    }

    @Test
    public void should_query_by_known_id() {
        // given
        // generateQueryTestData();

        // List<Long> validIds = jdbcTemplate.query("SELECT id FROM methods", (rs, rowNum) -> rs.getLong(1));

        // when
        // Optional<MethodDescriptor1> result = dashboardService.getMethodById(validIds.get(0));

        // then
        // assertThat(result.isPresent(), is(true));
    }

    @Test
    public void should_query_by_unknown_id() {
        // given
        // generateQueryTestData();
        setSecurityContextCustomerId(1L);

        // when
        Optional<MethodDescriptor1> result = dashboardService.getMethodById(-1L);

        // then
        assertThat(result.isPresent(), is(false));
    }

    private void setSecurityContextCustomerId(Long customerId) {
        if (customerId == null) {
            SecurityContextHolder.clearContext();
        } else {
            SecurityContext securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(new TestingAuthenticationToken(customerId, null));
            SecurityContextHolder.setContext(securityContext);
        }
    }


    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_getConfig_for_enabled_agent_1() {
        // given
        new Timestamps(jdbcTemplate).invoke();

        // when
        GetConfigResponse1 response = agentService.getConfig(
            GetConfigRequest1.sample().toBuilder()
                             .jvmUuid("uuid1")
                             .licenseKey("")
                             .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                             .build());

        // then
        assertConfigPollResponse(response, "enabled=true");

        // Assert all dead agents are marked as disabled as well
        assertAgentEnabled("uuid1", TRUE);
        assertAgentEnabled("uuid2", FALSE);
        assertAgentEnabled("uuid3", FALSE);
        assertAgentEnabled("uuid4", FALSE);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_getConfig_for_enabled_agent_2() {
        // given
        new Timestamps(jdbcTemplate).invoke();

        // when
        GetConfigResponse2 response = agentService.getConfig(
            GetConfigRequest2.sample().toBuilder()
                             .jvmUuid("uuid1")
                             .licenseKey("")
                             .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                             .build());

        // then
        assertConfigPollResponse(response, "enabled=true");

        // Assert all dead agents are marked as disabled as well
        assertAgentEnabled("uuid1", TRUE);
        assertAgentEnabled("uuid2", FALSE);
        assertAgentEnabled("uuid3", FALSE);
        assertAgentEnabled("uuid4", FALSE);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_getConfig_for_disabled_agent() {
        // given
        new Timestamps(jdbcTemplate).invoke();

        // when
        GetConfigResponse2 response = agentService.getConfig(
            GetConfigRequest2.sample().toBuilder()
                             .jvmUuid("uuid2")
                             .licenseKey("")
                             .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                             .build());

        // then
        assertConfigPollResponse(response, "enabled=false");

        assertAgentEnabled("uuid1", TRUE);
        assertAgentEnabled("uuid2", FALSE);
        assertAgentEnabled("uuid3", FALSE);
        assertAgentEnabled("uuid4", FALSE);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_recognize_enabled_agent_environment() {
        assertThat(agentDAO.isEnvironmentEnabled(1L, "uuid3"), is(true));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_recognize_disabled_agent_environment() {
        assertThat(agentDAO.isEnvironmentEnabled(1L, "uuid4"), is(false));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_treat_unknown_agent_environment_as_enabled() {
        assertThat(agentDAO.isEnvironmentEnabled(4711L, "foobar"), is(true));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void unknown_agent_environment_should_have_null_name() {
        assertThat(agentDAO.getEnvironmentName("foobar"), is(Optional.empty()));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_get_known_agent_environment_name() {
        assertThat(agentDAO.getEnvironmentName("uuid1"), is(Optional.of("env1")));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_getStatus_correctly() {
        // given
        Timestamps timestamps = new Timestamps(jdbcTemplate).invoke();
        setSecurityContextCustomerId(1L);

        // when
        GetStatusResponse status = dashboardService.getStatus();

        // then
        assertThat(status.getPricePlan(), is("DEMO"));
        assertThat(status.getCollectionResolutionSeconds(), is(PricePlanDefaults.DEMO.getPublishIntervalSeconds()));
        assertThat(status.getMaxNumberOfAgents(), is(PricePlanDefaults.DEMO.getMaxNumberOfAgents()));
        assertThat(status.getMaxNumberOfMethods(), is(100));

        assertThat(status.getNumAgents(), is(4));
        assertThat(status.getNumLiveAgents(), is(2));
        assertThat(status.getNumLiveEnabledAgents(), is(1));

        assertThat(status.getAgents().get(0), is(AgentDescriptor.builder()
                                                                .agentId(1L)
                                                                .agentAlive(true)
                                                                .agentLiveAndEnabled(true)
                                                                .agentVersion("agentVersion1")
                                                                .appName("app1")
                                                                .appVersion("v1")
                                                                .environment("env1")
                                                                .excludePackages("com.foobar.excluded1")
                                                                .hostname("hostname1")
                                                                .jvmId(1L)
                                                                .methodVisibility("public")
                                                                .nextPollExpectedAtMillis(cutMillis(timestamps.getInOneMinute()))
                                                                .nextPublicationExpectedAtMillis(cutMillis(
                                                                    Timestamp.from(timestamps.getTenMinutesAgo().toInstant().plusSeconds(
                                                                        PricePlanDefaults.DEMO.getPublishIntervalSeconds()))))
                                                                .packages("com.foobar1")
                                                                .pollReceivedAtMillis(cutMillis(timestamps.getTenMinutesAgo()))
                                                                .publishedAtMillis(cutMillis(timestamps.getTwoMinutesAgo()))
                                                                .startedAtMillis(cutMillis(timestamps.getAlmostThreeDaysAgo()))
                                                                .tags("tag1=t1,tag2=t2")
                                                                .build()));

        assertThat(status.getNumMethods(), is(10));

        assertThat(status.getCollectedSinceMillis(), is(nullValue()));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_getFilterData_for_known_customerId() {
        // given
        setSecurityContextCustomerId(1L);

        // when
        GetMethodsFormData getMethodsFormData = dashboardService.getMethodsFormData();

        // then
        assertThat(getMethodsFormData.getApplications(), contains("app1", "app2", "app3", "app4"));
        assertThat(getMethodsFormData.getEnvironments(), contains("env1", "env2", "env3", "env4"));
    }

    @Test(expected = AuthenticationException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_not_getFilterData_for_unknown_customerId() {
        // given
        setSecurityContextCustomerId(17L);

        // when
        dashboardService.getMethodsFormData();

        // then
        // kaboom!
    }

    @Test
    @Sql(scripts = {"/sql/base-data.sql", "/sql/garbage-data.sql"})
    public void should_perform_dataWeeding() {

        // given
        assertThat(countRowsInTable("invocations"), is(2));
        assertThat(countRowsInTable("applications"), is(5));
        assertThat(countRowsInTable("environments"), is(5));
        assertThat(countRowsInTable("method_locations"), is(6));
        assertThat(countRowsInTable("methods"), is(10));
        assertThat(countRowsInTable("jvms"), is(5));
        assertThat(countRowsInTable("agent_state"), is(5));

        // when
        weedingTask.performDataWeeding();

        // then
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTable("applications"), is(4));
        assertThat(countRowsInTable("environments"), is(4));
        assertThat(countRowsInTable("method_locations"), is(3));
        assertThat(countRowsInTable("methods"), is(1));
        assertThat(countRowsInTable("jvms"), is(4));
        assertThat(countRowsInTable("agent_state"), is(4));
    }

    @Test
    @Sql(scripts = {"/sql/base-data.sql", "/sql/weedable-data.sql"})
    public void should_find_weeding_candidates() {

        // given
        assertThat(countRowsInTable("applications"), not(is(0)));
        assertThat(countRowsInTable("environments"), not(is(0)));
        assertThat(countRowsInTable("methods"), not(is(0)));
        assertThat(countRowsInTable("method_locations"), not(is(0)));
        assertThat(countRowsInTable("jvms"), is(4));
        assertThat(countRowsInTable("agent_state"), is(4));

        // when
        weedingTask.performDataWeeding();

        // then
        assertThat(countRowsInTable("invocations"), is(0));
        assertThat(countRowsInTable("applications"), is(0));
        assertThat(countRowsInTable("environments"), is(0));
        assertThat(countRowsInTable("methods"), is(0));
        assertThat(countRowsInTable("method_locations"), is(0));
        assertThat(countRowsInTable("jvms"), is(0));
        assertThat(countRowsInTable("agent_state"), is(0));
    }

    @Test
    public void should_acquire_uncontended_lock() {
        Optional<Lock> lock = lockManager.acquireLock(Lock.forSystem());
        assertThat(lock.isPresent(), is(true));

        lock.ifPresent(lockManager::releaseLock);
    }

    @Test
    public void should_handle_lock_wait_timeout() throws InterruptedException {
        CountDownLatch[] latches = {new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)};

        new Thread(() -> lockContentionTestHelper.doSteps(latches)).start();

        latches[0].await();
        assertThat(lockManager.acquireLock(Lock.forSystem()).isPresent(), is(false));

        latches[1].countDown();

        latches[2].await();
        assertThat(lockManager.acquireLock(Lock.forSystem()).isPresent(), is(true));
    }

    @Test
    public void should_handle_lock_wait() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> lockContentionTestHelper.lockSleepUnlock(latch, Lock.forSystem().getMaxLockWaitSeconds() * 1000 / 2)).start();

        latch.await();
        assertThat(lockManager.acquireLock(Lock.forSystem()).isPresent(), is(true));
    }

    @RequiredArgsConstructor
    public static class LockContentionTestHelper {
        private final LockManager lockManager;

        @Transactional
        @SneakyThrows
        public void doSteps(CountDownLatch[] latches) {
            Optional<Lock> lock = lockManager.acquireLock(Lock.forSystem());
            latches[0].countDown();
            latches[1].await();
            lock.ifPresent(lockManager::releaseLock);
            latches[2].countDown();
        }

        @Transactional
        @SneakyThrows
        public void lockSleepUnlock(CountDownLatch latch, long sleepMillis) {
            Optional<Lock> lock = lockManager.acquireLock(Lock.forSystem());
            latch.countDown();
            Thread.sleep(sleepMillis);
            lock.ifPresent(lockManager::releaseLock);
        }
    }

    private void assertAgentEnabled(String jvmUuid, Boolean expectedEnabled) {
        Boolean enabled = jdbcTemplate.queryForObject("SELECT enabled FROM agent_state WHERE jvmUuid = ? ", Boolean.class, jvmUuid);
        assertThat(enabled, is(expectedEnabled));
    }

    private void assertConfigPollResponse(GetConfigResponse1 response, String publisherConfig) {
        PricePlanDefaults pp = PricePlanDefaults.DEMO;
        assertThat(response, is(GetConfigResponse1.sample().toBuilder()
                                                  .codeBasePublisherCheckIntervalSeconds(pp.getPublishIntervalSeconds())
                                                  .codeBasePublisherConfig(publisherConfig)
                                                  .codeBasePublisherName("http")
                                                  .codeBasePublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .configPollIntervalSeconds(pp.getPollIntervalSeconds())
                                                  .configPollRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .invocationDataPublisherConfig(publisherConfig)
                                                  .invocationDataPublisherIntervalSeconds(pp.getPublishIntervalSeconds())
                                                  .invocationDataPublisherName("http")
                                                  .invocationDataPublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .build()));
    }

    private void assertConfigPollResponse(GetConfigResponse2 response, String publisherConfig) {
        PricePlanDefaults pp = PricePlanDefaults.DEMO;
        assertThat(response, is(GetConfigResponse2.sample().toBuilder()
                                                  .codeBasePublisherCheckIntervalSeconds(pp.getPublishIntervalSeconds())
                                                  .codeBasePublisherConfig(publisherConfig)
                                                  .codeBasePublisherName("http")
                                                  .codeBasePublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .configPollIntervalSeconds(pp.getPollIntervalSeconds())
                                                  .configPollRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .invocationDataPublisherConfig(publisherConfig)
                                                  .invocationDataPublisherIntervalSeconds(pp.getPublishIntervalSeconds())
                                                  .invocationDataPublisherName("http")
                                                  .invocationDataPublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                                                  .build()));
    }

    private long cutMillis(Timestamp timestamp) {
        return Instant.ofEpochMilli(timestamp.getTime()).getEpochSecond() * 1000L;
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }

}
