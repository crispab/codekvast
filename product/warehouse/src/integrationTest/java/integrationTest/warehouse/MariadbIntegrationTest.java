package integrationTest.warehouse;

import integrationTest.warehouse.testdata.TestDataGenerator;
import org.flywaydb.core.Flyway;
import org.junit.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import io.codekvast.agent.lib.model.v1.legacy.ExportFileMetaInfo;
import io.codekvast.agent.lib.model.v1.legacy.JvmData;
import io.codekvast.agent.lib.model.v1.SignatureStatus;
import io.codekvast.testsupport.docker.DockerContainer;
import io.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import io.codekvast.warehouse.CodekvastWarehouse;
import io.codekvast.warehouse.file_import.legacy.LegacyImportDAO;
import io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.Application;
import io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.ImportContext;
import io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.ImportStatistics;
import io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.Invocation;
import io.codekvast.warehouse.file_import.legacy.ZipFileImporter;
import io.codekvast.warehouse.webapp.WebappService;
import io.codekvast.warehouse.webapp.model.GetMethodsRequest1;
import io.codekvast.warehouse.webapp.model.MethodDescriptor1;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.io.File;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static integrationTest.warehouse.testdata.ImportDescriptor.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.Jvm;
import static io.codekvast.warehouse.file_import.legacy.LegacyImportDAO.Method;

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
    private static final String DATABASE = "codekvast_warehouse";
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
    private ZipFileImporter importer;

    @Inject
    private LegacyImportDAO importDAO;

    @Inject
    private WebappService webappService;

    @Inject
    private TestDataGenerator testDataGenerator;

    private ImportContext importContext = new ImportContext();

    @Before
    public void beforeTest() throws Exception {
        assumeTrue(mariadb.isRunning());
    }

    @Test
    public void should_have_applied_all_flyway_migrations_to_an_empty_database() throws Exception {
        // given

        // when

        // then
        assertThat("Wrong number of applied Flyway migrations", flyway.info().applied().length, is(9));
        assertThat("Wrong number of pending Flyway migrations", flyway.info().pending().length, is(0));
    }

    @Test
    public void should_import_zipFile_idempotently() throws Exception {
        // given

        // when
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-1.zip"));
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-1.zip"));

        // then
        assertThat("Wrong number of import_file_info rows", countRowsInTable("import_file_info"), is(1));
        assertThat("Wrong number of applications rows", countRowsInTable("applications"), is(1));
        assertThat("Wrong number of jvms rows", countRowsInTable("jvms"), is(2));
        assertThat("Wrong number of methods rows", countRowsInTable("methods"), is(11));
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(11));
    }

    @Test
    @Ignore("Used only when tuning the SQL")
    public void should_import_large_zipFiles_fast() throws Exception {
        // given

        // when
        importer.importZipFile(getZipFile("/file_import/sample-jenkins-v1-1.zip"));
        importer.importZipFile(getZipFile("/file_import/sample-jenkins-v1-2.zip"));

        // then
        assertThat("Wrong number of import_file_info rows", countRowsInTable("import_file_info"), is(2));
        assertThat("Wrong number of applications rows", countRowsInTable("applications"), is(1));
        assertThat("Wrong number of jvms rows", countRowsInTable("jvms"), is(1));
        assertThat("Wrong number of methods rows", countRowsInTable("methods"), is(14423));
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(14423));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_all_signature_status_enum_values_correctly() throws Exception {
        // given

        // when
        int methodId = 0;
        for (SignatureStatus status : SignatureStatus.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, " +
                                        "invocationCount, status) VALUES(11, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

        // then
        assertThat("Wrong number of invocations rows", countRowsInTable("invocations"), is(SignatureStatus.values().length));
    }

    @Test
    public void should_reject_importing_same_file_twice() throws Exception {
        // given
        ExportFileMetaInfo metaInfo = ExportFileMetaInfo.createSampleExportFileMetaInfo();

        ImportStatistics stats = ImportStatistics.builder()
                                                 .fileSize("1.2 KB")
                                                 .importFile(File.createTempFile("codekvast-import", ".zip"))
                                                 .processingTime(Duration.ofMillis(100))
                                                 .build();

        assertThat(importDAO.isFileImported(metaInfo), is(false));

        // when
        importDAO.recordFileAsImported(metaInfo, stats);

        // then
        assertThat(importDAO.isFileImported(metaInfo), is(true));
        assertThat(countRowsInTable("import_file_info"), is(1));

        // when
        importDAO.recordFileAsImported(metaInfo, stats);

        // then
        assertThat(importDAO.isFileImported(metaInfo), is(true));
        assertThat(countRowsInTable("import_file_info"), is(1));
    }

    @Test
    public void should_import_same_application_only_once_with_the_same_central_id() throws Exception {
        // given
        Application app = createApplication(5L);

        // when
        boolean imported = importDAO.saveApplication(app, importContext);
        long centralId = importContext.getApplicationId(5L);

        // then
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(imported, is(true));
        assertThat(centralId, not(is(5L)));

        // given same app is imported again, from a different daemon
        app = app.toBuilder().localId(7L).build();

        // when
        imported = importDAO.saveApplication(app, importContext);

        // then
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(imported, is(false));
        assertThat(importContext.getApplicationId(7L), is(centralId));
    }

    @Test
    public void should_import_same_application_with_different_versions() throws Exception {
        // given
        Application app1 = createApplication(10L);

        // when
        boolean imported = importDAO.saveApplication(app1, importContext);

        // then
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(imported, is(true));
        assertThat(importContext.getApplicationId(10L), is(getCentralApplicationId(app1)));

        // given same app is imported again, with a different version
        Application app2 = app1.toBuilder().localId(11L).version("v2").build();

        // when
        imported = importDAO.saveApplication(app2, importContext);

        // then
        assertThat(countRowsInTable("applications"), is(2));
        assertThat(imported, is(true));

        assertThat(importContext.getApplicationId(10L), is(getCentralApplicationId(app1)));
        assertThat(importContext.getApplicationId(11L), is(getCentralApplicationId(app2)));
    }

    @Test
    public void should_import_or_update_jvm() throws Exception {
        // given
        JvmData jvmData = createJvmData(1000L, 2000L);
        Jvm jvm1 = createJvm(100L, jvmData);

        // when
        boolean imported = importDAO.saveJvm(jvm1, jvmData, importContext);

        // then
        assertThat(imported, is(true));
        assertThat(countRowsInTable("jvms"), is(1));
        assertThat(countRowsInTableWhere("jvms", "dumpedAt=?", new Timestamp(2000)), is(1));

        // given the same JVM is collected again
        Jvm jvm2 = jvm1.toBuilder().dumpedAtMillis(3000L).build();

        // when
        imported = importDAO.saveJvm(jvm2, jvmData, importContext);

        // then
        assertThat(imported, is(false));
        assertThat(countRowsInTable("jvms"), is(1));
        assertThat(countRowsInTableWhere("jvms", "dumpedAt=?", new Timestamp(2000)), is(0));
        assertThat(countRowsInTableWhere("jvms", "dumpedAt=?", new Timestamp(3000)), is(1));
    }

    private Jvm createJvm(long localId, JvmData jvmData) {
        return Jvm.builder()
                  .localId(localId)
                  .uuid(jvmData.getJvmUuid())
                  .jvmDataJson(jvmData.toString())
                  .startedAtMillis(jvmData.getStartedAtMillis())
                  .dumpedAtMillis(jvmData.getDumpedAtMillis())
                  .build();
    }

    @Test
    public void should_import_method() throws Exception {
        // given
        Method m1 = createMethod(100, "m1");

        // when
        boolean imported = importDAO.saveMethod(m1, importContext);

        // then
        assertThat(imported, is(true));
        assertThat(countRowsInTable("methods"), is(1));
        assertThat(importContext.getMethodId(100L), not(is(100L)));

        // given the same method is imported again with different localId
        Method m2 = m1.toBuilder().localId(1000L).build();

        // when
        imported = importDAO.saveMethod(m2, importContext);

        // then
        assertThat(imported, is(false));
        assertThat(countRowsInTable("methods"), is(1));
        assertThat(importContext.getMethodId(100L), not(is(100L)));
        assertThat(importContext.getMethodId(1000L), not(is(100L)));
        assertThat(importContext.getMethodId(1000L), not(is(1000L)));
    }

    @Test
    public void should_import_or_update_invocation() throws Exception {
        // given
        Application app = createApplication(10L);
        Method method = createMethod(20L, "signature");
        JvmData jvmData = createJvmData(1000L, 2000L);
        Jvm jvm = createJvm(100L, jvmData);
        importDAO.saveApplication(app, importContext);
        importDAO.saveMethod(method, importContext);
        importDAO.saveJvm(jvm, jvmData, importContext);

        Invocation inv1 = Invocation.builder()
                                    .localApplicationId(10L)
                                    .localJvmId(100L)
                                    .localMethodId(20L)
                                    .invocationCount(1L)
                                    .invokedAtMillis(10000L)
                                    .status(SignatureStatus.INVOKED)
                                    .build();

        // when
        importDAO.saveInvocation(inv1, importContext);

        // then
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", 10000L), is(1));

        // given the same invocation is saved with a newer timestamp
        Invocation inv2 = inv1.toBuilder().invokedAtMillis(20000L).build();

        // when
        importDAO.saveInvocation(inv2, importContext);

        // then
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv1.getInvokedAtMillis()), is(0));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv2.getInvokedAtMillis()), is(1));

        // given the same invocation is saved again with the same timestamp

        // when
        importDAO.saveInvocation(inv2, importContext);

        // given the same invocation is saved again but with and older timestamp
        Invocation inv3 = inv1.toBuilder().invokedAtMillis(inv2.getInvokedAtMillis() - 100).build();

        // when
        importDAO.saveInvocation(inv3, importContext);

        // then
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv1.getInvokedAtMillis()), is(0));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv2.getInvokedAtMillis()), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv3.getInvokedAtMillis()), is(0));
    }

    @Test
    public void should_query_by_IDEA_signature_correctly() throws Exception {
        // given
        generateQueryTestData();

        // when
        List<MethodDescriptor1> methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder()
                                  .signature(testDataGenerator.getMethod(1).getSignature().replace(".method", "#method"))
                                  .build());

        // then
        assertThat(methods, hasSize(1));

        MethodDescriptor1 md = methods.get(0);
        assertThat(toDaysAgo(md.getCollectedSinceMillis()), is(30));
        assertThat(toDaysAgo(md.getCollectedToMillis()), is(1));

    }

    @Test
    public void should_query_by_signature_suffix_correctly() throws Exception {
        // given
        generateQueryTestData();

        // when find substring
        List<MethodDescriptor1> methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder()
                                  .signature(testDataGenerator.getMethod(1).getSignature().substring(3))
                                  .build());

        // then
        assertThat(methods, hasSize(1));
    }

    @Test
    public void should_query_by_signature_not_normalize_but_no_match() throws Exception {
        // given
        generateQueryTestData();

        // when find by signature
        List<MethodDescriptor1> methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder()
                                  .signature(testDataGenerator.getMethod(1).getSignature().substring(1))
                                  .normalizeSignature(false)
                                  .build());

        // then
        assertThat(methods, hasSize(0));
    }

    @Test
    public void should_query_signatures_and_respect_max_results() throws Exception {
        // given
        generateQueryTestData();

        String signature = testDataGenerator.getMethod(0).getSignature();

        String prefix = signature.substring(signature.indexOf("integrationTest.warehouse"), signature.indexOf(".method"));
        assertThat(prefix, startsWith("integrationTest.warehouse"));
        assertThat(prefix, endsWith("TestClass1"));

        // when find many
        List<MethodDescriptor1> methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder()
                                  .signature(prefix)
                                  .build());

        // then
        assertThat(methods, hasSize(3));

        // when find many with max results
        methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder()
                                  .signature(prefix)
                                  .maxResults(2)
                                  .build());

        // then
        assertThat(methods, hasSize(2));
    }

    @Test(expected = ConstraintViolationException.class)
    public void should_throw_when_querying_signature_with_too_short_signature() throws Exception {
        // given
        generateQueryTestData();

        // when query with too short signature
        webappService.getMethods(GetMethodsRequest1.defaults().toBuilder().signature("").build());
    }

    @Test
    public void should_query_unknown_signature_correctly() throws Exception {
        // given
        generateQueryTestData();

        // when find exact signature
        List<MethodDescriptor1> methods = webappService.getMethods(
                GetMethodsRequest1.defaults().toBuilder().signature("foobar").build());

        // then
        assertThat(methods, hasSize(0));
    }

    @Test
    public void should_query_by_known_id() throws Exception {
        // given
        generateQueryTestData();

        List<Long> validIds = jdbcTemplate.query("SELECT id FROM methods", (rs, rowNum) -> rs.getLong(1));

        // when
        Optional<MethodDescriptor1> result = webappService.getMethodById(validIds.get(0));

        // then
        assertThat(result.isPresent(), is(true));
    }

    @Test
    public void should_query_by_unknown_id() throws Exception {
        // given
        generateQueryTestData();

        // when
        Optional<MethodDescriptor1> result = webappService.getMethodById(-1L);

        // then
        assertThat(result.isPresent(), is(false));
    }

    private void generateQueryTestData() {
        ImportDescriptorBuilder builder = builder()
                .now(now)

                .app("1 app1 1.0")
                .app("2 app2 2.0")
                .app("3 app3 3.0")

                .jvm(createJvm(1, adjust(now, -10, DAYS), adjust(now, -1, DAYS), "environment1", "host1", "tag1=1, tag2=1"))
                .jvm(createJvm(2, adjust(now, -30, DAYS), adjust(now, -29, DAYS), "environment2", "host2", "tag1=2, tag2=2"))
                .jvm(createJvm(3, adjust(now, -20, DAYS), adjust(now, -19, DAYS), "environment3", "host3", "tag1=3, tag2=3"));

        for (int i = 0; i < testDataGenerator.numMethods(); i++) {
            builder.method(testDataGenerator.getMethod(i));
        }

        for (long appId = 1; appId <= 3; appId++) {
            for (long methodId = 0; methodId < testDataGenerator.numMethods(); methodId++) {
                for (long jvmId = 1; jvmId <= 3; jvmId++) {

                    long hash = appId * 100 + methodId * 10 + jvmId;

                    builder.invocation(Invocation.builder()
                                                 .localApplicationId(appId)
                                                 .localMethodId(methodId)
                                                 .localJvmId(jvmId)
                                                 .status(SignatureStatus.INVOKED)
                                                 .invocationCount(hash)
                                                 .invokedAtMillis(hash)
                                                 .build());
                }
            }
        }
        testDataGenerator.simulateFileImport(builder.build());
    }

    private int toDaysAgo(long timestamp) {
        long days = 24 * 60 * 60 * 1000L;
        return Math.toIntExact((now - timestamp) / days);
    }

    private JvmDataPair createJvm(long localId, long startedAtMillis, long dumpedAtMillis, String environment, String hostName,
                                  String tags) {
        return new JvmDataPair(
                Jvm.builder()
                   .dumpedAtMillis(dumpedAtMillis)
                   .jvmDataJson("not-used")
                   .localId(localId)
                   .startedAtMillis(startedAtMillis)
                   .uuid("uuid" + localId)
                   .build(),
                JvmData.createSampleJvmData().toBuilder()
                       .collectorHostName(hostName)
                       .environment(environment)
                       .tags(tags)
                       .build()
        );
    }

    private long adjust(long instant, int duration, TemporalUnit unit) {
        return Instant.ofEpochMilli(instant).plus(duration, unit).toEpochMilli();
    }

    private Application createApplication(long localId) {
        return Application.builder()
                          .localId(localId)
                          .name("application")
                          .version("v1")
                          .createdAtMillis(System.currentTimeMillis())
                          .build();
    }

    private Method createMethod(long localId, String signature) {
        return Method.builder()
                     .localId(localId)
                     .visibility("visibility")
                     .signature(signature)
                     .createdAtMillis(System.currentTimeMillis())
                     .declaringType("declaringType")
                     .exceptionTypes("exceptionTypes")
                     .methodName("methodName")
                     .modifiers("modifiers")
                     .packageName("packageName")
                     .parameterTypes("parameterTypes")
                     .returnType("returnType")
                     .build();
    }

    private JvmData createJvmData(Long startedAtMillis, long dumpedAtMillis) {
        return JvmData.createSampleJvmData().toBuilder()
                      .jvmUuid(UUID.randomUUID().toString())
                      .dumpedAtMillis(dumpedAtMillis)
                      .startedAtMillis(startedAtMillis)
                      .build();
    }

    private Long getCentralApplicationId(Application app) {
        return jdbcTemplate
                .queryForList("SELECT id FROM applications WHERE name=? AND version=?", Long.class, app.getName(), app.getVersion()).get(0);
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

    private int countRowsInTableWhere(String tableName, String where, Object... args) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + tableName + " WHERE " + where, Integer.class, args);
    }
}
