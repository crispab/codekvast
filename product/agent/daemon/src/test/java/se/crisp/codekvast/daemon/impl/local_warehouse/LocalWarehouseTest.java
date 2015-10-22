package se.crisp.codekvast.daemon.impl.local_warehouse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.daemon.DataProcessor;
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
    private DataProcessor dataProcessor;

    private final String[][] signatureParts = {
            {"public", "pkg1.pkg2.pkg3.Class1.method1(java.lang.String, int)"},
            {"private", "Class2.method2()"}
    };
    private List<String> signatures = makeSignatures(signatureParts);

    private long t1 = System.currentTimeMillis();
    private long t2 = t1 + 20000L;
    private long t3 = t1 + 60000L;

    private CollectorConfig collectorConfig1;
    private JvmState jvmState1;
    private CodeBase codeBase1;

    @Before
    public void before() throws Exception {
        collectorConfig1 = createCollectorConfig("appName1", "appVersion1");
        jvmState1 = createJvmState(collectorConfig1, "jvm1", t1);
        codeBase1 = mockCodeBase(collectorConfig1);
    }

    private List<String> makeSignatures(String[][] signatureParts) {
        List<String> result = new ArrayList<String>();
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
        when(codeBase.getSignatures()).thenReturn(new HashSet<String>(signatures));
        for (String s : signatures) {
            when(codeBase.normalizeSignature(s)).thenReturn(s);
            when(codeBase.getBaseSignature(s)).thenReturn(s);
            when(codeBase.hasSignature(s)).thenReturn(true);
        }

        return codeBase;
    }

    @Test
    public void testProcessData() {
        // given (note that the invocation files are in reverse order!)
        int dumpNumber = 0;
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, t2, singleton(signatures.get(1)));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, t1, singleton(signatures.get(1)));

        // when
        dataProcessor.processData(t1, jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM applications WHERE name = ? AND version = ? AND createdAtMillis = ? ",
                                               Integer.class, collectorConfig1.getAppName(), collectorConfig1.getAppVersion(), t1),
                   is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods", Integer.class), is(2));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods WHERE visibility = ? AND signature = ? ",
                                               Integer.class, signatureParts[0][0], signatureParts[0][1]), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods WHERE visibility = ? AND signature = ? ",
                                               Integer.class, signatureParts[1][0], signatureParts[1][1]), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", t1, t2), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, t2, 1L),
                   is(1));

        // Simulate that the collector dumps again...

        // given
        jvmState1.setJvm(jvmState1.getJvm().withDumpedAtMillis(t3));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), ++dumpNumber, t3, singleton(signatures.get(1)));

        // when
        dataProcessor.processData(t3 + 10000, jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", t1, t3), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ?  ",
                                               Integer.class, t3, 2), is(1));
    }
}
