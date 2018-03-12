package integrationTest.dashboard;

import integrationTest.dashboard.testdata.TestDataGenerator;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.CustomerService.LoginRequest;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.dashboard.CodekvastDashboardApplication;
import io.codekvast.dashboard.agent.AgentService;
import io.codekvast.dashboard.dashboard.DashboardService;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor;
import io.codekvast.dashboard.dashboard.model.status.AgentDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.javaagent.model.v1.CodeBaseEntry;
import io.codekvast.javaagent.model.v1.CodeBasePublication;
import io.codekvast.javaagent.model.v1.CommonPublicationData;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.*;
import io.codekvast.testsupport.docker.DockerContainer;
import io.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import org.flywaydb.core.Flyway;
import org.junit.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "ClassWithTooManyFields"})
@SpringBootTest(
    classes = {CodekvastDashboardApplication.class, TestDataGenerator.class},
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
    private CustomerService customerService;

    @Inject
    private DashboardService dashboardService;

    @Inject
    private AgentService agentService;

    @Inject
    private CodeBaseImporter codeBaseImporter;

    @Inject
    private InvocationDataImporter invocationDataImporter;

    @Inject
    private TestDataGenerator testDataGenerator;

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
        assertThat(countRowsInTable("schema_version WHERE success != 1"), is(0));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_all_signature_status_enum_values_correctly() {
        // given

        // when
        int methodId = 0;
        for (SignatureStatus2 status : SignatureStatus2.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(customerId, applicationId, methodId, jvmId, invokedAtMillis, " +
                                    "invocationCount, status) VALUES(1, 11, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

        // then
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(SignatureStatus2.values().length));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByCustomerId() {
        CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);
        assertThat(customerData.getCustomerId(), is(1L));
        assertThat(customerData.getCustomerName(), is("Demo"));
        assertThat(customerData.getPricePlan().getName(), is("DEMO"));
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
        CustomerData customerData = customerService.getCustomerDataByExternalId("external-1");
        assertThat(customerData.getCustomerId(), is(1L));
        assertThat(customerData.getCustomerName(), is("Demo"));
        assertThat(customerData.getPricePlan().getName(), is("DEMO"));
        assertThat(customerData.getPricePlan().getOverrideBy(), is("integration test"));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByExternalId_without_pricePlanOverride() {
        CustomerData customerData = customerService.getCustomerDataByExternalId("external-2");
        assertThat(customerData.getCustomerId(), is(2L));
        assertThat(customerData.getPricePlan().getOverrideBy(), nullValue());
        assertThat(customerData.isTrialPeriodExpired(Instant.now()), is(false));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_valid_getCustomerDataByExternalId_trialPeriodExpired() {
        Instant now = Instant.parse("2017-09-21T16:21:19Z").plus(1, DAYS); // see base-data.sql
        CustomerData customerData = customerService.getCustomerDataByExternalId("external-3");
        assertThat(customerData.getCustomerId(), is(3L));
        assertThat(customerData.isTrialPeriodExpired(now), is(true));
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_invalid_getCustomerDataByExternalId() {
        customerService.getCustomerDataByExternalId("undefined");
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_add_delete_customer() {
        String licenseKey = customerService.addCustomer(CustomerService.AddCustomerRequest
                                                            .builder()
                                                            .source("test")
                                                            .externalId("externalId")
                                                            .name("customerName")
                                                            .plan("test")
                                                            .build());

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers WHERE externalId = ?", Long.class, "externalId");

        assertThat(count, is(1L));

        assertThat(licenseKey, notNullValue());

        customerService.deleteCustomerByExternalId("externalId");

        count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers WHERE externalId = ?", Long.class, "externalId");
        assertThat(count, is(0L));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_delete_customer() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers", Long.class);
        assertThat(count, is(3L));

        customerService.deleteCustomerByExternalId("external-1");

        count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM customers", Long.class);
        assertThat(count, is(2L));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_handle_register_login_twice() {

        // given
        jdbcTemplate.update("DELETE FROM users");

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

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_register_interactive_activity() {

        // given
        jdbcTemplate.update("DELETE FROM users");

        // when
        customerService.registerLogin(LoginRequest.builder()
                                                  .customerId(1L)
                                                  .source("source1")
                                                  .email("email")
                                                  .build());

        // then
        assertThat(countRowsInTable("users"), is(1));
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_publication_invalid_licenseKey() {
        customerService.assertPublicationSize("undefined", 10);
    }

    @Test(expected = LicenseViolationException.class)
    @Sql(scripts = "/sql/base-data.sql")
    public void should_reject_publication_too_large() {
        customerService.assertPublicationSize("", 100_000);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_accept_publication() {
        customerService.assertPublicationSize("", 10);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_assertDatabaseSize() {
        customerService.assertDatabaseSize(1L);
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_start_trial_period_at_first_agent_publishing() {
        // given
        Instant now = Instant.now();
        jdbcTemplate.update("UPDATE customers SET plan = 'test', collectionStartedAt = NULL, trialPeriodEndsAt = NULL WHERE id = 1");
        CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);

        assertThat(customerData.getPricePlan().getMaxCollectionPeriodDays(), is(PricePlanDefaults.TEST.getMaxCollectionPeriodDays()));
        assertThat(customerData.getCollectionStartedAt(), is(nullValue()));
        assertThat(customerData.getTrialPeriodEndsAt(), is(nullValue()));
        assertThat(customerData.isTrialPeriodExpired(now), is(false));

        // when
        customerData = customerService.registerAgentDataPublication(customerData, now);

        // then
        assertThat(customerData.getCollectionStartedAt(), is(now));
        int days = customerData.getPricePlan().getMaxCollectionPeriodDays();
        assertThat(customerData.getTrialPeriodEndsAt(), is(now.plus(days, DAYS)));
        assertThat(customerData.isTrialPeriodExpired(now.plus(days - 1, DAYS)), is(false));
        assertThat(customerData.isTrialPeriodExpired(now.plus(days + 1, DAYS)), is(true));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void should_import_codeBasePublication1() {
        //@formatter:off
        CodeBasePublication publication = CodeBasePublication.builder()
            .commonData(CommonPublicationData.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry.sampleCodeBaseEntry()))
            .overriddenSignatures(Collections.singletonMap("signature", "overriddenBySignature"))
            .strangeSignatures(Collections.singletonMap("rawStrangeSignature", "normalizedStrangeSignature"))
            .build();
        //@formatter:on

        codeBaseImporter.importPublication(CodeBasePublication2.fromV1Format(publication));
    }

    @Test
    public void should_import_codeBasePublication2() {
        //@formatter:off
        CodeBasePublication2 publication = CodeBasePublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry2.sampleCodeBaseEntry()))
            .build();
        //@formatter:on

        codeBaseImporter.importPublication(publication);
    }

    @Test
    public void should_import_codeBasePublication2_after_invocationDataPublication() {
        //@formatter:off
        CodeBasePublication2 codeBasePublication = CodeBasePublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(Arrays.asList(CodeBaseEntry2.sampleCodeBaseEntry()))
            .build();

        InvocationDataPublication2 invocationDataPublication = InvocationDataPublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .recordingIntervalStartedAtMillis(System.currentTimeMillis())
            .invocations(codeBasePublication.getEntries().stream().map(CodeBaseEntry2::getSignature).collect(Collectors.toSet()))
            .build();
        //@formatter:on

        invocationDataImporter.importPublication(invocationDataPublication);
        codeBaseImporter.importPublication(codeBasePublication);
    }

    @Test
    public void should_import_invocationDataPublication() {
        //@formatter:off
        InvocationDataPublication2 publication = InvocationDataPublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .recordingIntervalStartedAtMillis(System.currentTimeMillis())
            .invocations(Collections.singleton("signature"))
            .build();
        //@formatter:on

        invocationDataImporter.importPublication(publication);
    }

    @Test(expected = ConstraintViolationException.class)
    public void should_throw_when_querying_signature_with_too_short_signature() {
        // given

        // when query with too short signature
        dashboardService.getMethods(GetMethodsRequest.defaults().toBuilder().signature("").build());
    }

    @Test
    public void should_query_unknown_signature_correctly() {
        // given
        setSecurityContextCustomerId(1L);

        // when find exact signature
        GetMethodsResponse response = dashboardService.getMethods(
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
        // Optional<MethodDescriptor> result = dashboardService.getMethodById(validIds.get(0));

        // then
        // assertThat(result.isPresent(), is(true));
    }

    @Test
    public void should_query_by_unknown_id() {
        // given
        // generateQueryTestData();
        setSecurityContextCustomerId(1L);

        // when
        Optional<MethodDescriptor> result = dashboardService.getMethodById(-1L);

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
    public void should_getConfig_for_enabled_agent() {
        // given
        new Timestamps().invoke();

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
    public void should_getConfig_for_disabled_agent() {
        // given
        new Timestamps().invoke();

        // when
        GetConfigResponse1 response = agentService.getConfig(
            GetConfigRequest1.sample().toBuilder()
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
    public void should_getStatus_correctly() {
        // given
        Timestamps timestamps = new Timestamps().invoke();
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
                                                                .agentAlive(true)
                                                                .agentLiveAndEnabled(true)
                                                                .agentVersion("agentVersion1")
                                                                .appName("app1")
                                                                .appVersion("v1")
                                                                .environment("env1")
                                                                .excludePackages("com.foobar.excluded1")
                                                                .id(1L)
                                                                .methodVisibility("public")
                                                                .nextPollExpectedAtMillis(cutMillis(timestamps.plusOneMinute))
                                                                .nextPublicationExpectedAtMillis(cutMillis(
                                                                     Timestamp.from(timestamps.minusTenMinutes.toInstant().plusSeconds(
                                                                         PricePlanDefaults.DEMO.getPublishIntervalSeconds()))))
                                                                .packages("com.foobar1")
                                                                .pollReceivedAtMillis(cutMillis(timestamps.minusTenMinutes))
                                                                .publishedAtMillis(cutMillis(timestamps.minusTwoMinutes))
                                                                .startedAtMillis(cutMillis(timestamps.minusThreeDaysPlus))
                                                                .tags("tag1=t1,tag2=t2")
                                                                .build()));

        assertThat(status.getNumMethods(), is(10));

        assertThat(status.getCollectedSinceMillis(), is(nullValue()));
        assertThat(status.getCollectedDays(), is(nullValue()));

        assertThat(status.getUsers(), hasSize(2));
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

    private long cutMillis(Timestamp timestamp) {
        return Instant.ofEpochMilli(timestamp.getTime()).getEpochSecond() * 1000L;
    }

    private int countRowsInTable(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }

    private class Timestamps {
        Timestamp minusThreeDaysPlus;
        Timestamp minusTenMinutes;
        Timestamp minusTwoMinutes;
        Timestamp plusOneMinute;

        Timestamps invoke() {
            // Set the timestamps from Java. It's impossible to write time-zone agnostic code in a static sql script invoked by @Sql.

            Instant now = Instant.now();
            minusThreeDaysPlus = Timestamp.from(now.minus(3, DAYS).minus(5, HOURS));
            minusTenMinutes = Timestamp.from(now.minus(10, MINUTES));
            minusTwoMinutes = Timestamp.from(now.minus(2, MINUTES));
            plusOneMinute = Timestamp.from(now.plus(1, MINUTES));

            jdbcTemplate.update("UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
                                minusTenMinutes, plusOneMinute, TRUE, "uuid1");

            jdbcTemplate.update("UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
                                minusTenMinutes, plusOneMinute, FALSE, "uuid2");

            jdbcTemplate.update("UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
                                minusTenMinutes, minusTwoMinutes, TRUE, "uuid3");

            jdbcTemplate.update("UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
                                minusTenMinutes, minusTwoMinutes, FALSE, "uuid4");

            jdbcTemplate.update("UPDATE jvms SET startedAt = ?, publishedAt = ?", minusTenMinutes, minusTwoMinutes);
            jdbcTemplate.update("UPDATE jvms SET startedAt = ? WHERE uuid = ?", minusThreeDaysPlus, "uuid1");
            return this;
        }
    }
}
