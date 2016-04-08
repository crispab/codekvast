package se.crisp.codekvast.warehouse;

import org.flywaydb.core.Flyway;
import org.junit.Before;
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
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.agent.lib.model.v1.JvmData;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.testsupport.docker.DockerContainer;
import se.crisp.codekvast.testsupport.docker.MariaDbContainerReadyChecker;
import se.crisp.codekvast.warehouse.file_import.ImportDAO;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.Application;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.ImportContext;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.ImportStatistics;
import se.crisp.codekvast.warehouse.file_import.ImportDAO.Invocation;
import se.crisp.codekvast.warehouse.file_import.ZipFileImporter;

import javax.inject.Inject;
import java.io.File;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static se.crisp.codekvast.warehouse.file_import.ImportDAO.Jvm;
import static se.crisp.codekvast.warehouse.file_import.ImportDAO.Method;

/**
 * @author olle.hallin@crisp.se
 */
@SpringApplicationConfiguration(classes = CodekvastWarehouse.class)
@IntegrationTest
@ActiveProfiles({"integrationTest"})
@Transactional
public class MariadbIntegrationTest {

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
    private ImportDAO importDAO;

    private ImportContext importContext = new ImportContext();

    @Before
    public void beforeTest() throws Exception {
        assumeTrue(mariadb.isRunning());
    }

    @Test
    public void should_apply_all_flyway_migrations_to_an_empty_database() throws Exception {
        // given

        // when

        // then
        assertThat(flyway.info().applied().length, is(9));
        assertThat(flyway.info().pending().length, is(0));
    }

    @Test
    public void should_import_zipFile_idempotently() throws Exception {
        // given

        // when
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-1.zip"));
        importer.importZipFile(getZipFile("/file_import/sample-ltw-v1-1.zip"));

        // then
        assertThat(countRowsInTable("import_file_info"), is(1));
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(countRowsInTable("invocations"), is(11));
        assertThat(countRowsInTable("jvms"), is(2));
        assertThat(countRowsInTable("methods"), is(11));
    }

    @Test
    @Sql(scripts = "/sql/base-data.sql")
    public void should_store_all_signature_statuses_correctly() throws Exception {
        // given

        // when
        int methodId = 0;
        long now = System.currentTimeMillis();
        for (SignatureStatus status : SignatureStatus.values()) {
            methodId += 1;
            jdbcTemplate.update("INSERT INTO invocations(applicationId, methodId, jvmId, invokedAtMillis, " +
                                        "invocationCount, status) VALUES(11, ?, 1, ?, 0, ?)",
                                methodId, now, status.toString());
        }

        // then
        assertThat(countRowsInTable("invocations"), is(SignatureStatus.values().length));
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
    public void should_import_same_application_only_once() throws Exception {
        // given
        Application app = createApplication(10L);

        // when
        boolean imported = importDAO.saveApplication(app, importContext);

        // then
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(imported, is(true));
        assertThat(importContext.getApplicationId(10L), is(1L));

        // given same app is imported again, from a different daemon
        app = app.toBuilder().localId(200L).build();

        // when
        imported = importDAO.saveApplication(app, importContext);

        // then
        assertThat(countRowsInTable("applications"), is(1));
        assertThat(imported, is(false));
        assertThat(importContext.getApplicationId(200L), is(1L));
    }

    @Test
    public void should_import_application_with_different_versions() throws Exception {
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
                                    .status(SignatureStatus.EXACT_MATCH)
                                    .build();

        // when
        boolean imported = importDAO.saveInvocation(inv1, importContext);

        // then
        assertThat(imported, is(true));
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", 10000L), is(1));

        // given the same invocation is saved with a newer timestamp
        Invocation inv2 = inv1.toBuilder().invokedAtMillis(20000L).build();

        // when
        imported = importDAO.saveInvocation(inv2, importContext);

        // then
        assertThat(imported, is(true));
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv1.getInvokedAtMillis()), is(0));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv2.getInvokedAtMillis()), is(1));

        // given the same invocation is saved again with the same timestamp

        // when
        imported = importDAO.saveInvocation(inv2, importContext);

        // then
        assertThat(imported, is(false));

        // given the same invocation is saved again but with and older timestamp
        Invocation inv3 = inv1.toBuilder().invokedAtMillis(inv2.getInvokedAtMillis() - 100).build();

        // when
        imported = importDAO.saveInvocation(inv3, importContext);

        // then
        assertThat(imported, is(false));
        assertThat(countRowsInTable("invocations"), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv1.getInvokedAtMillis()), is(0));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv2.getInvokedAtMillis()), is(1));
        assertThat(countRowsInTableWhere("invocations", "invokedAtMillis=?", inv3.getInvokedAtMillis()), is(0));
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
