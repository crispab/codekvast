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
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
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

    private final String signature1 = "public signature1()";
    private final String signature2 = "private signature2()";
    private Set<String> signatures = new HashSet<String>(asList(signature1, signature2));

    private long now1 = System.currentTimeMillis();

    private CollectorConfig collectorConfig1;
    private JvmState jvmState1;
    private CodeBase codeBase1;

    @Before
    public void before() throws Exception {
        collectorConfig1 = createCollectorConfig("appName1", "appVersion1");
        jvmState1 = createJvmState(collectorConfig1, "jvm1", now1);
        codeBase1 = mockCodeBase(collectorConfig1);
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
        when(codeBase.getSignatures()).thenReturn(signatures);
        for (String s : signatures) {
            when(codeBase.normalizeSignature(s)).thenReturn(s);
            when(codeBase.getBaseSignature(s)).thenReturn(s);
            when(codeBase.hasSignature(s)).thenReturn(true);
        }

        return codeBase;
    }

    @Test
    public void testProcessData() {
        // given
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), 1, now1 + 20000, new HashSet<String>(asList(signature2)));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), 2, now1, new HashSet<String>(asList(signature2)));

        // when
        dataProcessor.processData(now1, jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM applications WHERE name = ? AND version = ? AND createdAtMillis = ? ",
                                               Integer.class, collectorConfig1.getAppName(), collectorConfig1.getAppVersion(), now1),
                   is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods", Integer.class), is(2));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM methods WHERE visibility = ? AND signature = ? ", Integer.class,
                                               "public", "signature1()"), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", now1, now1 + 20000), is(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, now1 + 20000, 1L),
                   is(1));

        // given
        jvmState1.setJvm(jvmState1.getJvm().withDumpedAtMillis(now1 + 60000));
        FileUtils.writeInvocationDataTo(jvmState1.getInvocationsFile(), 3, now1 + 60000, new HashSet<String>(asList(signature2)));

        // when
        dataProcessor.processData(now1 + 60000, jvmState1, codeBase1);

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jvms WHERE uuid = ? AND startedAtMillis = ? AND dumpedAtMillis = ?",
                                               Integer.class, "jvm1", now1, now1 + 60000), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ? ",
                                               Integer.class, -1L, 0L), is(1));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invocations WHERE invokedAtMillis = ? AND invocationCount = ?  ",
                                               Integer.class, now1 + 60000, 2), is(1));
    }
}
