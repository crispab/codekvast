package se.crisp.codekvast.agent.main;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.main.appversion.AppVersionStrategy;
import se.crisp.codekvast.agent.main.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiException;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastAgentIntegTest
public class AgentWorkerIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private final String JVM_UUID = UUID.randomUUID().toString();
    private long jvmStartedAtMillis = System.currentTimeMillis() - 60000L;
    private long now = System.currentTimeMillis();

    @Inject
    public InvocationsCollector invocationsCollector;

    @Mock
    private AgentApi agentApi;

    @Mock
    private AgentWorker.TransactionHelper transactionHelper;

    private AgentWorker worker;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(agentApi.getServerUri()).thenReturn(new URI("http://server"));

        List<AppVersionStrategy> appVersionStrategies = new ArrayList<AppVersionStrategy>();
        AgentConfig agentConfig = createAgentConfig();

        worker = new AgentWorker(agentApi, agentConfig, scanner, appVersionStrategies, invocationsCollector, transactionHelper);
    }

    @Test
    public void testUploadSignatureData_uploadSameCodeBaseOnlyOnce() throws AgentApiException, IOException {
        // given
        thereIsCollectorDataFromJvm(JVM_UUID, "codebase1", now - 4711L);

        // when
        worker.analyseCollectorData();
        worker.analyseCollectorData();

        // then
        verify(agentApi, times(1)).uploadSignatureData(any(JvmData.class), anyCollectionOf(String.class));
    }

    @Test
    public void testUploadSignatureData_shouldRetryOnFailure() throws AgentApiException, IOException {
        // given
        thereIsCollectorDataFromJvm(JVM_UUID, "codebase1", now - 4711L);

        doThrow(new AgentApiException("Failed to contact server")).doNothing()
                                                                  .when(agentApi).uploadSignatureData(any(JvmData.class),
                                                                                                      anyCollectionOf(String.class));

        // when
        worker.analyseCollectorData();
        worker.analyseCollectorData();
        worker.analyseCollectorData();

        // then
        verify(agentApi, times(2)).uploadSignatureData(any(JvmData.class), anyCollectionOf(String.class));
    }

    private void thereIsCollectorDataFromJvm(String jvmUuid, String codebase, long dumpedAtMillis) throws IOException {
        CollectorConfig cc = CollectorConfig.builder()
                                            .appName("appName")
                                            .appVersion("appVersion")
                                            .codeBase("src/test/resources/agentWorkerTest/" + codebase)
                                            .dataPath(temporaryFolder.getRoot())
                                            .packagePrefixes("org, sample")
                                            .methodVisibility("methodVisibility")
                                            .tags("tags")
                                            .build();

        Jvm jvm = Jvm.builder()
                     .collectorConfig(cc)
                     .computerId("computerId")
                     .dumpedAtMillis(dumpedAtMillis)
                     .hostName("hostName")
                     .jvmUuid(jvmUuid)
                     .startedAtMillis(jvmStartedAtMillis)
                     .build();

        jvm.saveTo(cc.getJvmFile());
    }

    private AgentConfig createAgentConfig() {
        try {
            return AgentConfig.builder()
                              .apiAccessID("accessId")
                              .apiAccessSecret("secret")
                              .dataPath(temporaryFolder.getRoot())
                              .serverUploadIntervalSeconds(60)
                              .serverUri(new URI("http://localhost:8090"))
                              .agentVcsId("git-hash")
                              .agentVersion("version")
                              .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
