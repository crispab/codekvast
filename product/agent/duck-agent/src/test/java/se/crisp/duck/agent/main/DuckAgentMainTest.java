package se.crisp.duck.agent.main;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.agent.util.SensorUtils;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DuckAgentMainTest {

    @InjectMocks
    private AgentWorker agentWorker;

    @Test
    public void testApplyRecordedUsage() throws Exception {
        CodeBase codeBase = new CodeBase(getConfig("build/classes/main"));
        codeBase.readScannerResult(getResource("/customer1/app1/signatures.dat"));
        assertTrue(codeBase.hasSignature(
                "public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));

        int unrecognized = agentWorker.applyRecordedUsage(codeBase, new AppUsage("appName"),
                                                          SensorUtils.readUsageFrom(getResource("/customer1/app1/usage.dat")));

        assertThat(unrecognized, is(1));
    }

    private File getResource(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

    private AgentConfig getConfig(String codeBase) {
        return AgentConfig.builder().codeBaseUri(new File(codeBase).toURI()).build();
    }

}
