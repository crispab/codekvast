package se.crisp.codekvast.daemon.impl.local_warehouse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.daemon.CollectorDataProcessor;
import se.crisp.codekvast.daemon.DataExportException;
import se.crisp.codekvast.daemon.DataExporter;
import se.crisp.codekvast.daemon.DataProcessingException;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.main.LocalWarehouseIntegrationTest;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.config.CollectorConfigFactory;
import se.crisp.codekvast.shared.model.Jvm;
import se.crisp.codekvast.shared.util.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringJUnit4ClassRunner.class)
@LocalWarehouseIntegrationTest
public class LocalWarehouseTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private CollectorDataProcessor collectorDataProcessor;

    @Inject
    public DataExporter dataExporter;

    @Inject
    public DaemonConfig config;

    private static final long T1 = System.currentTimeMillis();
    private static final long T2 = T1 + 20000L;
    private static final long T3 = T1 + 60000L;

    private static final String[][] SIGNATURE_PARTS = {
            {"public", "pkg1.pkg2.pkg3.Class1.method1(java.lang.String, int)"},
            {"private", "Class2.method2()"}
    };
    private static final List<String> SIGNATURES = makeSignatures(SIGNATURE_PARTS);

    private CollectorConfig collectorConfig1;
    private JvmState jvmState1;
    private CodeBase codeBase1;

    @Before
    public void before() throws Exception {
        collectorConfig1 = createCollectorConfig("appName1", "appVersion1");
        jvmState1 = createJvmState(collectorConfig1, "jvm1", T1);
        codeBase1 = mockCodeBase(collectorConfig1);
    }

    private static List<String> makeSignatures(String[][] signatureParts) {
        List<String> result = new ArrayList<>();
        for (String[] parts : signatureParts) {
            result.add(String.format("%s %s", parts[0], parts[1]));
        }
        return result;
    }

    private JvmState createJvmState(CollectorConfig collectorConfig, String jvmUuid, long startedAtMillis) throws IOException {
        JvmState jvmState = new JvmState();
        jvmState.setJvm(Jvm.builder()
                           .collectorConfig(collectorConfig)
                           .collectorVcsId("collectorVcsId")
                           .collectorVersion("collectorVersion")
                           .computerId("computerId")
                           .hostName("hostName")
                           .jvmUuid(jvmUuid)
                           .startedAtMillis(startedAtMillis)
                           .dumpedAtMillis(startedAtMillis + 20000)
                           .build());

        jvmState.setInvocationsFile(new File(temporaryFolder.newFolder(), "invocations.dat"));
        return jvmState;
    }

    private CollectorConfig createCollectorConfig(String appName, String appVersion) {
        return CollectorConfigFactory.builder()
                                     .appName(appName).appVersion(appVersion).codeBase("codeBase").packagePrefixes("packagePrefixes")
                                     .build();
    }

    private CodeBase mockCodeBase(CollectorConfig collectorConfig) {
        CodeBase codeBase = mock(CodeBase.class);

        when(codeBase.getUrls()).thenReturn(new URL[]{});
        when(codeBase.getConfig()).thenReturn(collectorConfig);
        when(codeBase.getSignatures()).thenReturn(new HashSet<>(SIGNATURES));
        for (String s : SIGNATURES) {
            when(codeBase.normalizeSignature(s)).thenReturn(s);
            when(codeBase.getBaseSignature(s)).thenReturn(s);
            when(codeBase.hasSignature(s)).thenReturn(true);
        }

        return codeBase;
    }

    @Test
    public void testProcessData() throws DataProcessingException, DataExportException {
        // given (note that the invocation files are in reverse order!)
        int dumpNumber = 0;
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, T2, singleton(SIGNATURES.get(1)));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, T1, singleton(SIGNATURES.get(1)));

        // when
        collectorDataProcessor.processCollectorData(jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM applications WHERE name = ? AND version = ? AND createdAtMillis = ? ",
                                               Integer.class, collectorConfig1.getAppName(), collectorConfig1.getAppVersion(), T1),
                   is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods", Integer.class), is(2));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods WHERE visibility = ? AND signature = ? ",
                                               Integer.class, SIGNATURE_PARTS[0][0], SIGNATURE_PARTS[0][1]), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods WHERE visibility = ? AND signature = ? ",
                                               Integer.class, SIGNATURE_PARTS[1][0], SIGNATURE_PARTS[1][1]), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", T1, T2), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, T2, 1L),
                   is(1));

        // Simulate that the collector dumps again...

        // given
        jvmState1.setJvm(jvmState1.getJvm().withDumpedAtMillis(T3));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, T3, singleton(SIGNATURES.get(1)));

        // when
        collectorDataProcessor.processCollectorData(jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", T1, T3), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ?  ",
                                               Integer.class, T3, 2), is(1));

        dataExporter.exportData();

        assertThat(config.getExportFile().exists(), is(true));
    }
}
