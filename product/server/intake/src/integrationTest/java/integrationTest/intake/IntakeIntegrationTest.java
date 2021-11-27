package integrationTest.intake;

import static io.codekvast.javaagent.model.v2.SignatureStatus2.INVOKED;
import static io.codekvast.javaagent.model.v2.SignatureStatus2.NOT_INVOKED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
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
import io.codekvast.common.messaging.DuplicateMessageIdException;
import io.codekvast.common.messaging.impl.MessageIdRepository;
import io.codekvast.common.metrics.CommonMetricsService;
import io.codekvast.database.DatabaseLimits;
import io.codekvast.intake.CodekvastIntakeApplication;
import io.codekvast.intake.agent.service.AgentService;
import io.codekvast.intake.agent.service.impl.IntakeDAO;
import io.codekvast.intake.file_import.CodeBaseImporter;
import io.codekvast.intake.file_import.InvocationDataImporter;
import io.codekvast.intake.file_import.PublicationImporter;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.CodeBaseEntry2;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import io.codekvast.javaagent.model.v3.CodeBaseEntry3;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
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
      CodekvastIntakeApplication.class,
      IntakeIntegrationTest.LockContentionTestHelper.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationTest")
@Transactional
public class IntakeIntegrationTest {

  @ClassRule public static final SpringClassRule springClassRule = new SpringClassRule();
  private static final String DATABASE = "codekvast";
  private static final String USERNAME = "codekvastUser";
  private static final String PASSWORD = "codekvastPassword";
  private static final String SYNTHETIC_SIGNATURE =
      "customer1.FooConfig..EnhancerBySpringCGLIB..96aac875.CGLIB$BIND_CALLBACKS(java.lang.Object)";

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

  @Inject private AgentService agentService;

  @Inject private IntakeDAO intakeDAO;

  @Inject private PublicationImporter publicationImporter;

  @Inject private CodeBaseImporter codeBaseImporter;

  @Inject private InvocationDataImporter invocationDataImporter;

  @Inject private LockManager lockManager;

  @Inject private LockContentionTestHelper lockContentionTestHelper;

  @Inject private MessageIdRepository messageIdRepository;

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
    assertThat(countRowsInTable("codebase_fingerprints"), is(0));
    assertThat(countRowsInTable("packages"), is(0));
    assertThat(countRowsInTable("types"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("method_locations"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    CodeBaseEntry2 entry1 = CodeBaseEntry2.sampleCodeBaseEntry();
    CodeBasePublication2 publication =
        CodeBasePublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(asList(entry1, entry1.toBuilder().signature(SYNTHETIC_SIGNATURE).build()))
            .build();
    File file = writeToTempFile(publication);

    // when
    publicationImporter.importPublicationFile(file);

    // then
    assertThat(
        countRowsInTable("applications WHERE name = ?", publication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", publication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", publication.getCommonData().getJvmUuid()), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry1.getSignature()), is(1));
    assertThat(
        countRowsInTable(
            "codebase_fingerprints WHERE codeBaseFingerprint = ?",
            publication.getCommonData().getCodeBaseFingerprint()),
        is(1));
    assertThat(
        countRowsInTable("method_locations"), is(0)); // location is not supported in CodeBaseEntry2
    assertThat(
        countRowsInTable("packages WHERE name = ?", entry1.getMethodSignature().getPackageName()),
        is(1));
    assertThat(
        countRowsInTable("types WHERE name = ?", entry1.getMethodSignature().getDeclaringType()),
        is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry1.getSignature()), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", SYNTHETIC_SIGNATURE), is(0));
    assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", NOT_INVOKED.name()), is(1));
  }

  @Test
  public void should_import_codeBasePublication3() {
    // given
    assertThat(countRowsInTable("applications"), is(0));
    assertThat(countRowsInTable("environments"), is(0));
    assertThat(countRowsInTable("jvms"), is(0));
    assertThat(countRowsInTable("packages"), is(0));
    assertThat(countRowsInTable("types"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    CodeBaseEntry3 entry1 = CodeBaseEntry3.sampleCodeBaseEntry();
    CodeBasePublication3 publication =
        CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(asList(entry1, entry1.toBuilder().signature(SYNTHETIC_SIGNATURE).build()))
            .build();

    // when
    codeBaseImporter.importPublication(publication);

    // then
    assertThat(
        countRowsInTable("applications WHERE name = ?", publication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", publication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", publication.getCommonData().getJvmUuid()), is(1));
    assertThat(
        countRowsInTable("packages WHERE name = ?", entry1.getMethodSignature().getPackageName()),
        is(1));
    assertThat(
        countRowsInTable("types WHERE name = ?", entry1.getMethodSignature().getDeclaringType()),
        is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry1.getSignature()), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", SYNTHETIC_SIGNATURE), is(0));
    assertThat(
        countRowsInTable(
            "method_locations WHERE location = ?", entry1.getMethodSignature().getLocation()),
        is(1));
    assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", NOT_INVOKED.name()), is(1));
  }

  @Test
  public void should_ignore_already_imported_codeBasePublication3() {
    // given
    assertThat(countRowsInTable("applications"), is(0));
    assertThat(countRowsInTable("environments"), is(0));
    assertThat(countRowsInTable("jvms"), is(0));
    assertThat(countRowsInTable("codebase_fingerprints"), is(0));
    assertThat(countRowsInTable("packages"), is(0));
    assertThat(countRowsInTable("types"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    CodeBaseEntry3 entry1 = CodeBaseEntry3.sampleCodeBaseEntry();
    CodeBaseEntry3 entry2 =
        CodeBaseEntry3.sampleCodeBaseEntry().toBuilder()
            .signature(entry1.getSignature() + "2")
            .build();

    long now = System.currentTimeMillis();

    CodeBasePublication3 publication1 =
        CodeBasePublication3.builder()
            .commonData(
                CommonPublicationData2.sampleCommonPublicationData().toBuilder()
                    .codeBaseFingerprint("fingerprint1")
                    .publishedAtMillis(now)
                    .build())
            .entries(asList(entry1))
            .build();

    // when
    codeBaseImporter.importPublication(publication1);

    // then
    assertThat(countRowsInTable("methods"), is(1));
    assertThat(
        countRowsInTable(
            "codebase_fingerprints WHERE publishedAt = ?",
            Timestamp.from(Instant.ofEpochMilli(now).truncatedTo(ChronoUnit.MILLIS))),
        is(1));

    // given
    CodeBasePublication3 publication2 =
        CodeBasePublication3.builder()
            .commonData(
                CommonPublicationData2.sampleCommonPublicationData().toBuilder()
                    .codeBaseFingerprint("fingerprint1")
                    .publishedAtMillis(now + 1000)
                    .build())
            .entries(asList(entry1, entry2))
            .build();

    // when
    codeBaseImporter.importPublication(publication2);

    // then
    assertThat(countRowsInTable("methods"), is(1));
    assertThat(
        countRowsInTable(
            "codebase_fingerprints WHERE publishedAt = ?",
            Timestamp.from(Instant.ofEpochMilli(now + 1000).truncatedTo(ChronoUnit.MILLIS))),
        is(1));
  }

  @Test
  public void should_import_codeBasePublication3_with_too_long_signature_entry() {
    // given
    assertThat(countRowsInTable("applications"), is(0));
    assertThat(countRowsInTable("environments"), is(0));
    assertThat(countRowsInTable("jvms"), is(0));
    assertThat(countRowsInTable("packages"), is(0));
    assertThat(countRowsInTable("types"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    CodeBaseEntry3 entry1 = CodeBaseEntry3.sampleCodeBaseEntry();
    CodeBaseEntry3 entry2 = entry1.toBuilder().signature(SYNTHETIC_SIGNATURE).build();
    int signatureLength = DatabaseLimits.MAX_METHOD_SIGNATURE_LENGTH + 1;
    int indexLength = DatabaseLimits.MAX_METHOD_SIGNATURE_INDEX_LENGTH;
    CodeBaseEntry3 entry3 =
        entry1.toBuilder()
            .signature(RandomStringUtils.randomAlphanumeric(signatureLength - 2) + "()")
            .build();

    CodeBasePublication3 publication =
        CodeBasePublication3.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .entries(asList(entry1, entry2, entry3, entry3))
            .build();

    // when
    codeBaseImporter.importPublication(publication);

    // then
    assertThat(
        countRowsInTable("applications WHERE name = ?", publication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", publication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", publication.getCommonData().getJvmUuid()), is(1));
    assertThat(
        countRowsInTable("packages WHERE name = ?", entry1.getMethodSignature().getPackageName()),
        is(1));
    assertThat(
        countRowsInTable("types WHERE name = ?", entry1.getMethodSignature().getDeclaringType()),
        is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry1.getSignature()), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry2.getSignature()), is(0));
    assertThat(countRowsInTable("methods WHERE signature = ?", entry3.getSignature()), is(0));
    assertThat(
        countRowsInTable("truncated_signatures WHERE signature = ?", entry3.getSignature()), is(1));

    assertThat(
        countRowsInTable(
            "methods WHERE signature LIKE ?",
            entry3.getSignature().substring(0, signatureLength - 3) + "...%"),
        is(0));
    assertThat(
        countRowsInTable(
            "methods WHERE signature LIKE ?",
            entry3.getSignature().substring(0, indexLength) + "%"),
        is(1));

    assertThat(
        countRowsInTable(
            "method_locations WHERE location = ?", entry1.getMethodSignature().getLocation()),
        is(2));
    assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(2));
    assertThat(countRowsInTable("invocations WHERE status = ?", NOT_INVOKED.name()), is(2));
  }

  @Test
  public void should_import_invocationDataPublication_then_codeBasePublication() {
    // given
    assertThat(countRowsInTable("applications"), is(0));
    assertThat(countRowsInTable("environments"), is(0));
    assertThat(countRowsInTable("jvms"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    CommonPublicationData2 commonData = CommonPublicationData2.sampleCommonPublicationData();
    CodeBaseEntry3 codeBaseEntry = CodeBaseEntry3.sampleCodeBaseEntry();
    CodeBasePublication3 codeBasePublication =
        CodeBasePublication3.builder()
            .commonData(commonData)
            .entries(asList(codeBaseEntry))
            .build();
    val signature = codeBaseEntry.getSignature();
    long intervalStartedAtMillis = System.currentTimeMillis();

    InvocationDataPublication2 invocationDataPublication =
        InvocationDataPublication2.builder()
            .commonData(commonData)
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis)
            .invocations(singleton(signature))
            .build();

    // when
    invocationDataImporter.importPublication(invocationDataPublication);

    // then
    assertThat(
        countRowsInTable(
            "applications WHERE name = ?", codeBasePublication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", codeBasePublication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", codeBasePublication.getCommonData().getJvmUuid()),
        is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", signature), is(1));
    assertThat(countRowsInTable("method_locations"), is(0));
    assertThat(countRowsInTable("invocations"), is(1));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", INVOKED.name()), is(1));

    // given
    long intervalStartedAtMillis2 = intervalStartedAtMillis + 3600;

    // when
    invocationDataImporter.importPublication(
        invocationDataPublication.toBuilder()
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis2)
            .build());

    // then
    assertThat(countRowsInTable("invocations"), is(1));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis2), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", INVOKED.name()), is(1));

    // when
    codeBaseImporter.importPublication(codeBasePublication);

    // then
    assertThat(
        countRowsInTable(
            "applications WHERE name = ?", codeBasePublication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", codeBasePublication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", codeBasePublication.getCommonData().getJvmUuid()),
        is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", signature), is(1));
    assertThat(
        countRowsInTable(
            "method_locations WHERE location = ?",
            codeBaseEntry.getMethodSignature().getLocation()),
        is(1));
    assertThat(countRowsInTable("invocations"), is(1));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis2), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", INVOKED.name()), is(1));
  }

  @Test
  public void should_import_codeBasePublication_then_invocationDataPublication_twice() {
    // given
    assertThat(countRowsInTable("applications"), is(0));
    assertThat(countRowsInTable("environments"), is(0));
    assertThat(countRowsInTable("jvms"), is(0));
    assertThat(countRowsInTable("methods"), is(0));
    assertThat(countRowsInTable("invocations"), is(0));

    String signature1 = "signature1";
    String signature2 = "signature2";
    CodeBaseEntry3 codeBaseEntry1 =
        CodeBaseEntry3.sampleCodeBaseEntry().toBuilder().signature(signature1).build();
    CodeBaseEntry3 codeBaseEntry2 =
        CodeBaseEntry3.sampleCodeBaseEntry().toBuilder().signature(signature2).build();
    CommonPublicationData2 commonData = CommonPublicationData2.sampleCommonPublicationData();
    CodeBasePublication3 codeBasePublication =
        CodeBasePublication3.builder()
            .commonData(commonData)
            .entries(asList(codeBaseEntry1, codeBaseEntry2))
            .build();

    // when Import a code base with two distinct signatures
    codeBaseImporter.importPublication(codeBasePublication);

    // then
    assertThat(
        countRowsInTable(
            "applications WHERE name = ?", codeBasePublication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", codeBasePublication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", codeBasePublication.getCommonData().getJvmUuid()),
        is(1));
    assertThat(countRowsInTable("methods"), is(2));
    assertThat(countRowsInTable("methods WHERE signature = ?", signature1), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", signature2), is(1));
    assertThat(countRowsInTable("method_locations"), is(2));
    assertThat(countRowsInTable("invocations"), is(2));
    assertThat(countRowsInTable("invocations WHERE invokedAtMillis = 0"), is(2));
    assertThat(countRowsInTable("invocations WHERE status = ?", NOT_INVOKED.name()), is(2));

    // given
    long intervalStartedAtMillis1 = System.currentTimeMillis();
    InvocationDataPublication2 invocationDataPublication1 =
        InvocationDataPublication2.builder()
            .commonData(commonData)
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis1)
            .invocations(singleton(signature1))
            .build();
    long intervalStartedAtMillis2 = intervalStartedAtMillis1 + 3600;
    InvocationDataPublication2 invocationDataPublication2 =
        invocationDataPublication1.toBuilder()
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis2)
            .build();
    // when Record the invocation of one of the signatures twice in reversed time order
    invocationDataImporter.importPublication(invocationDataPublication2);
    invocationDataImporter.importPublication(invocationDataPublication1);

    // then
    assertThat(countRowsInTable("invocations"), is(2));
    assertThat(countRowsInTable("invocations WHERE invokedAtMillis = ?", 0), is(1));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis1), is(0));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis2), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", NOT_INVOKED.name()), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", INVOKED.name()), is(1));

    // given
    setSecurityContextCustomerId(commonData.getCustomerId());
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

    InvocationDataPublication2 publication =
        InvocationDataPublication2.builder()
            .commonData(CommonPublicationData2.sampleCommonPublicationData())
            .recordingIntervalStartedAtMillis(intervalStartedAtMillis)
            .invocations(new HashSet<>(asList("signature", SYNTHETIC_SIGNATURE)))
            .build();

    // when
    invocationDataImporter.importPublication(publication);

    // then
    assertThat(
        countRowsInTable("applications WHERE name = ?", publication.getCommonData().getAppName()),
        is(1));
    assertThat(
        countRowsInTable(
            "environments WHERE name = ?", publication.getCommonData().getEnvironment()),
        is(1));
    assertThat(
        countRowsInTable("jvms WHERE uuid = ?", publication.getCommonData().getJvmUuid()), is(1));
    assertThat(countRowsInTable("methods"), is(1));
    assertThat(countRowsInTable("methods WHERE signature = ?", "signature"), is(1));
    assertThat(countRowsInTable("invocations"), is(1));
    assertThat(
        countRowsInTable("invocations WHERE invokedAtMillis = ?", intervalStartedAtMillis), is(1));
    assertThat(countRowsInTable("invocations WHERE status = ?", INVOKED.name()), is(1));
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
    GetConfigResponse1 response =
        agentService.getConfig1(
            GetConfigRequest1.sample().toBuilder()
                .jvmUuid("uuid1")
                .licenseKey("")
                .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                .build());

    // then
    assertConfigPollResponse(response);

    // Assert all dead agents are marked as disabled as well
    assertAgentEnabledAndGarbage("uuid1", TRUE, FALSE);
    assertAgentEnabledAndGarbage("uuid2", FALSE, FALSE);
    assertAgentEnabledAndGarbage("uuid3", TRUE, TRUE);
    assertAgentEnabledAndGarbage("uuid4", FALSE, TRUE);
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_getConfig_for_enabled_agent_2() {
    // given
    new Timestamps(jdbcTemplate).invoke();

    // when
    GetConfigResponse2 response =
        agentService.getConfig2(
            GetConfigRequest2.sample().toBuilder()
                .jvmUuid("uuid1")
                .licenseKey("")
                .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                .build());

    // then
    assertConfigPollResponse(response, "enabled=true");

    // Assert all dead agents are marked as garbage
    assertAgentEnabledAndGarbage("uuid1", TRUE, FALSE);
    assertAgentEnabledAndGarbage("uuid2", FALSE, FALSE);
    assertAgentEnabledAndGarbage("uuid3", TRUE, TRUE);
    assertAgentEnabledAndGarbage("uuid4", FALSE, TRUE);
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_getConfig_for_disabled_agent() {
    // given
    new Timestamps(jdbcTemplate).invoke();

    // when
    GetConfigResponse2 response =
        agentService.getConfig2(
            GetConfigRequest2.sample().toBuilder()
                .jvmUuid("uuid2")
                .licenseKey("")
                .startedAtMillis(Instant.now().minus(2, HOURS).toEpochMilli())
                .build());

    // then
    assertConfigPollResponse(response, "enabled=false");

    assertAgentEnabledAndGarbage("uuid1", TRUE, FALSE);
    assertAgentEnabledAndGarbage("uuid2", FALSE, FALSE);
    assertAgentEnabledAndGarbage("uuid3", TRUE, TRUE);
    assertAgentEnabledAndGarbage("uuid4", FALSE, TRUE);
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_recognize_enabled_agent_environment() {
    assertThat(intakeDAO.isEnvironmentEnabled(1L, "uuid3"), is(true));
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_recognize_disabled_agent_environment() {
    assertThat(intakeDAO.isEnvironmentEnabled(1L, "uuid4"), is(false));
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_treat_unknown_agent_environment_as_enabled() {
    assertThat(intakeDAO.isEnvironmentEnabled(4711L, "foobar"), is(true));
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void unknown_agent_environment_should_have_null_name() {
    assertThat(intakeDAO.getEnvironmentName("foobar"), is(Optional.empty()));
  }

  @Test
  @Sql(scripts = "/sql/base-data.sql")
  public void should_get_known_agent_environment_name() {
    assertThat(intakeDAO.getEnvironmentName("uuid1"), is(Optional.of("env1")));
  }

  @Test
  public void should_accept_new_messageId() throws DuplicateMessageIdException {
    // when
    messageIdRepository.rememberMessageId(UUID.randomUUID().toString());

    // then
    // no exception
  }

  @Test(expected = DuplicateMessageIdException.class)
  public void should_reject_duplicate_messageId() throws DuplicateMessageIdException {
    // given
    String messageId = UUID.randomUUID().toString();
    messageIdRepository.rememberMessageId(messageId);

    // when
    messageIdRepository.rememberMessageId(messageId);

    // then
    // Exception!
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

  private void assertAgentEnabledAndGarbage(
      String jvmUuid, Boolean expectedEnabled, Boolean expectedEnabledGarbage) {
    Boolean enabled =
        jdbcTemplate.queryForObject(
            "SELECT enabled FROM agent_state WHERE jvmUuid = ? ", Boolean.class, jvmUuid);
    assertThat(enabled, is(expectedEnabled));

    Boolean garbage =
        jdbcTemplate.queryForObject(
            "SELECT garbage FROM agent_state WHERE jvmUuid = ? ", Boolean.class, jvmUuid);
    assertThat(garbage, is(expectedEnabledGarbage));
  }

  private void assertConfigPollResponse(GetConfigResponse1 response) {
    PricePlanDefaults pp = PricePlanDefaults.DEMO;
    assertThat(
        response,
        is(
            GetConfigResponse1.sample().toBuilder()
                .codeBasePublisherCheckIntervalSeconds(pp.getPublishIntervalSeconds())
                .codeBasePublisherConfig("enabled=true")
                .codeBasePublisherName("http")
                .codeBasePublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                .configPollIntervalSeconds(pp.getPollIntervalSeconds())
                .configPollRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                .invocationDataPublisherConfig("enabled=true")
                .invocationDataPublisherIntervalSeconds(pp.getPublishIntervalSeconds())
                .invocationDataPublisherName("http")
                .invocationDataPublisherRetryIntervalSeconds(pp.getRetryIntervalSeconds())
                .build()));
  }

  private void assertConfigPollResponse(GetConfigResponse2 response, String publisherConfig) {
    PricePlanDefaults pp = PricePlanDefaults.DEMO;
    assertThat(
        response,
        is(
            GetConfigResponse2.sample().toBuilder()
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
