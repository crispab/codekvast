package se.crisp.codekvast.daemon.impl.local_warehouse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.daemon.DataProcessor;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.main.LocalWarehouseIntegrationTest;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.config.CollectorConfigFactory;
import se.crisp.codekvast.shared.model.Jvm;

import javax.inject.Inject;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(SpringJUnit4ClassRunner.class)
@LocalWarehouseIntegrationTest
public class LocalWarehouseTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private DataProcessor dataProcessor;

    @Mock
    private CodeBase codeBase;

    private Set<String> signatures = new HashSet<String>(asList("public com.acme.foo()",
                                                                "private com.acme.bar(java.lang.String)"));
    private long now1 = System.currentTimeMillis();
    private long now2 = now1 + 60000;

    private JvmState jvmState = new JvmState();
    private CollectorConfig collectorConfig = CollectorConfigFactory.createSampleCollectorConfig();

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(codeBase.getUrls()).thenReturn(new URL[]{});
        when(codeBase.getConfig()).thenReturn(collectorConfig);
        when(codeBase.getSignatures()).thenReturn(signatures);


        jvmState.setAppVersion("appVersion");
        jvmState.setJvm(Jvm.builder()
                           .collectorConfig(collectorConfig)
                           .collectorVcsId("collectorVcsId")
                           .collectorVersion("collectorVersion")
                           .computerId("computerId")
                           .hostName("hostName")
                           .jvmUuid("jvmUuid")
                           .startedAtMillis(now1)
                           .build());
    }

    @Test
    public void testStoreJvmFirstTime() {
        dataProcessor.processData(now1, jvmState, codeBase);
    }
}
