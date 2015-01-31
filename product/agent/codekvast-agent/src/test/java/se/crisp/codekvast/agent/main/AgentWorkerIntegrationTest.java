package se.crisp.codekvast.agent.main;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.agent.config.AgentConfig;
import se.crisp.codekvast.agent.config.SharedConfig;
import se.crisp.codekvast.agent.main.appversion.AppVersionStrategy;
import se.crisp.codekvast.agent.main.appversion.LiteralAppVersionStrategy;
import se.crisp.codekvast.agent.main.codebase.CodeBaseScanner;
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiException;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AgentWorkerIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<>();
    private final ComputerID computerId = ComputerID.compute();
    @Mock
    private AgentApi agentApi;
    @Mock
    private CodeBaseScanner scanner;

    private AgentWorker worker;

    @Before
    public void before() throws Exception {
        appVersionStrategies.add(new LiteralAppVersionStrategy());

        AgentConfig agentConfig = createAgentConfig(temporaryFolder.getRoot());
        worker = new AgentWorker("codekvastVersion", "gitHash", agentApi, agentConfig, scanner, appVersionStrategies, computerId);
    }

    private AgentConfig createAgentConfig(File dataPath) {
        try {
            return AgentConfig.builder().sharedConfig(SharedConfig.builder().dataPath(dataPath).build())
                              .apiAccessID("accessId")
                              .apiAccessSecret("secret")
                              .serverUploadIntervalSeconds(60)
                              .serverUri(new URI("http://localhost:8090"))
                              .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testResolveConstantAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "constant foo"), is("foo"));
    }

    @Test
    public void testResolveLiteralAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "   LITERAL    foo"), is("foo"));
    }

    @Test
    public void testResolveUnrecognizedAppVersion() throws Exception {
        assertThat(AgentWorker.resolveAppVersion(appVersionStrategies, null, "   FOOBAR    foo   "), is("FOOBAR    foo"));
    }

    @Test
    @Ignore("Work in progress")
    public void testUploadSameCodeBaseOnlyOnce() throws AgentApiException {
        // given
        // what?

        // when
        worker.analyseCollectorData();
        worker.analyseCollectorData();

        // then
        verify(agentApi, times(1)).uploadSignatureData(anyString(), anyCollection());
    }

}
