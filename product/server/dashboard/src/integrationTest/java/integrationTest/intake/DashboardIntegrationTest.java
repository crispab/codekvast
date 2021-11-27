package integrationTest.intake;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeTrue;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.CustomerService.LoginRequest;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.customer.PricePlanDefaults;
import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockManager;
import io.codekvast.common.metrics.CommonMetricsService;
import io.codekvast.dashboard.CodekvastDashboardApplication;
import io.codekvast.dashboard.dashboard.DashboardService;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsFormData;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsRequest;
import io.codekvast.dashboard.dashboard.model.methods.GetMethodsResponse2;
import io.codekvast.dashboard.dashboard.model.methods.MethodDescriptor1;
import io.codekvast.dashboard.dashboard.model.status.AgentDescriptor;
import io.codekvast.dashboard.dashboard.model.status.GetStatusResponse;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.RabbitMQContainer;

/** @author olle.hallin@crisp.se */
@SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "ClassWithTooManyFields"})
@SpringBootTest(
    classes = {
      CodekvastDashboardApplication.class,
      DashboardIntegrationTest.LockContentionTestHelper.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
@Transactional
public class DashboardIntegrationTest {

  @ClassRule public static final SpringClassRule springClassRule = new SpringClassRule();
  private static final String DATABASE = "codekvast";
  private static final String USERNAME = "codekvastUser";
  private static final String PASSWORD = "codekvastPassword";

  @ClassRule
  public static MariaDBContainer<?> mariaDB =
      new MariaDBContainer<>("mariadb:10.4")
          .withDatabaseName(DATABASE)
          .withUsername(USERNAME)
          .withPassword(PASSWORD)
          .withEnv("MYSQL_INITDB_SKIP_TZINFO", "true");

  @ClassRule
  public static RabbitMQContainer rabbitMQ =
      new RabbitMQContainer("rabbitmq:3.8-management-alpine")
          .withVhost("/")
          .withUser("admin", "secret");

  private final Set<Optional<Lock>> heldLocks = new HashSet<>();
  @Rule public SpringMethodRule springMethodRule = new SpringMethodRule();

  @MockBean private CommonMetricsService commonMetricsService;

  @Inject private JdbcTemplate jdbcTemplate;

  @Inject private Flyway flyway;

  @Inject private CustomerService customerService;

  @Inject private DashboardService dashboardService;

  @Inject private LockManager lockManager;

  @Inject private LockContentionTestHelper lockContentionTestHelper;

  @BeforeClass
  public static void beforeClass() {
    System.setProperty("spring.datasource.url", mariaDB.getJdbcUrl());
    System.setProperty("spring.datasource.username", USERNAME);
    System.setProperty("spring.datasource.password", PASSWORD);
    System.setProperty("spring.rabbitmq.addresses", rabbitMQ.getAmqpUrl());
  }

  private Optional<Lock> acquireLock(Lock lock) {
    Optional<Lock> result = lockManager.acquireLock(lock);
    heldLocks.add(result);
    return result;
  }

  @Before
  public void beforeTest() {
    assumeTrue(mariaDB.isRunning());
    assumeTrue(rabbitMQ.isRunning());
  }

  @After
  public void afterTest() {
    // Evict the customer cache
    try {
      customerService.changePlanForExternalId("test", "external-1", "demo");
    } catch (Exception ignore) {
      // do nothing
    }

    setSecurityContextCustomerId(null);
    for (Optional<Lock> heldLock : heldLocks) {
      heldLock.ifPresent(lockManager::releaseLock);
    }
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
      jdbcTemplate.update(
          "INSERT INTO invocations(customerId, applicationId, environmentId, methodId, invokedAtMillis, status) VALUES(1, 1, 1, ?, "
              + "0, ?)",
          methodId,
          status.toString());
    }

    // then
    assertThat(
        "Wrong number of invocations rows",
        countRowsInTable("invocations"),
        is(SignatureStatus2.values().length));
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
    CustomerService.AddCustomerResponse response =
        customerService.addCustomer(
            CustomerService.AddCustomerRequest.builder()
                .source("source")
                .externalId("externalId")
                .name("customerName")
                .plan("test")
                .build());

    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM customers WHERE source = ? AND externalId = ?",
            Long.class,
            "source",
            "externalId");

    assertThat(count, is(1L));

    assertThat(response.getLicenseKey(), notNullValue());

    customerService.deleteCustomerByExternalId("source", "externalId");

    count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM customers WHERE source= ? AND externalId = ?",
            Long.class,
            "source",
            "externalId");
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
    customerService.registerLogin(
        LoginRequest.builder().customerId(1L).source("source1").email("email").build());

    customerService.registerLogin(
        LoginRequest.builder().customerId(1L).source("source2").email("email").build());

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
    customerService.changePlanForExternalId("test", "external-1", "test");
    CustomerData customerData = customerService.getCustomerDataByCustomerId(1L);

    assertThat(
        customerData.getPricePlan().getTrialPeriodDays(),
        is(PricePlanDefaults.TEST.getTrialPeriodDays()));
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

    assertThat(
        customerData.getPricePlan().getTrialPeriodDays(),
        is(PricePlanDefaults.DEMO.getTrialPeriodDays()));
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
  public void should_query_unknown_signature_correctly() {
    // given
    setSecurityContextCustomerId(1L);

    // when find exact signature
    GetMethodsResponse2 response =
        dashboardService.getMethods2(
            GetMethodsRequest.defaults().toBuilder().signature("foobar").build());

    // then
    assertThat(response.getMethods(), hasSize(0));
  }

  @Test
  public void should_query_by_known_id() {
    // given
    // generateQueryTestData();

    // List<Long> validIds = jdbcTemplate.query("SELECT id FROM methods", (rs, rowNum) ->
    // rs.getLong(1));

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
  public void should_getStatus_correctly() {
    // given
    Timestamps timestamps = new Timestamps(jdbcTemplate).invoke();
    setSecurityContextCustomerId(1L);

    // when
    GetStatusResponse status = dashboardService.getStatus();

    // then
    assertThat(status.getPricePlan(), is("DEMO"));
    assertThat(
        status.getCollectionResolutionSeconds(),
        is(PricePlanDefaults.DEMO.getPublishIntervalSeconds()));
    assertThat(status.getMaxNumberOfAgents(), is(PricePlanDefaults.DEMO.getMaxNumberOfAgents()));
    assertThat(status.getMaxNumberOfMethods(), is(100));

    assertThat(status.getNumAgents(), is(4));
    assertThat(status.getNumLiveAgents(), is(2));
    assertThat(status.getNumLiveEnabledAgents(), is(1));

    assertThat(
        status.getAgents().get(0),
        is(
            AgentDescriptor.builder()
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
                .nextPublicationExpectedAtMillis(
                    cutMillis(
                        Timestamp.from(
                            timestamps
                                .getTenMinutesAgo()
                                .toInstant()
                                .plusSeconds(PricePlanDefaults.DEMO.getPublishIntervalSeconds()))))
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
    assertThat(getMethodsFormData.getLocations(), contains("loc1", "loc2", "loc3"));
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
  public void should_acquire_free_lock() {
    Optional<Lock> lock = acquireLock(Lock.forTask("test", 60));
    assertThat(lock.isPresent(), is(true));

    lock.ifPresent(lockManager::releaseLock);
  }

  @Test
  public void should_handle_lock_wait_timeout() throws InterruptedException {
    Lock lock = Lock.forTask("test", 60);
    CountDownLatch[] latches = {
      new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)
    };

    new Thread(() -> lockContentionTestHelper.doSteps(lock, latches)).start();

    latches[0].await();
    assertThat(acquireLock(lock).isPresent(), is(false));

    latches[1].countDown();

    latches[2].await();
    assertThat(acquireLock(lock).isPresent(), is(true));
  }

  @Test
  public void should_wait_for_lock() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Lock lock = Lock.forCustomer(1L);
    new Thread(() -> lockContentionTestHelper.lockSleepUnlock(lock, latch)).start();

    latch.await();
    assertThat(acquireLock(lock).isPresent(), is(true));
  }

  private long cutMillis(Timestamp timestamp) {
    return Instant.ofEpochMilli(timestamp.getTime()).getEpochSecond() * 1000L;
  }

  private int countRowsInTable(String tableName, Object... args) {
    Integer result =
        jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class, args);
    return (result != null ? result : 0);
  }

  @RequiredArgsConstructor
  public static class LockContentionTestHelper {

    private final LockManager lockManager;

    @Transactional
    @SneakyThrows
    public void doSteps(Lock lock, CountDownLatch[] latches) {
      Optional<Lock> acquiredLock = lockManager.acquireLock(lock);
      latches[0].countDown();
      latches[1].await();
      acquiredLock.ifPresent(lockManager::releaseLock);
      latches[2].countDown();
    }

    @Transactional
    @SneakyThrows
    public void lockSleepUnlock(Lock lock, CountDownLatch latch) {
      Optional<Lock> acquiredLock = lockManager.acquireLock(lock);
      latch.countDown();
      Thread.sleep(500);
      acquiredLock.ifPresent(lockManager::releaseLock);
    }
  }
}
