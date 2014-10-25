package se.crisp.codekvast.agent.main;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.agent.util.AgentConfig;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent.ServerDelegate;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class CodekvastAgentMainTest {

    private AgentWorker agentWorker =
            new AgentWorker(AgentConfig.createSampleConfiguration(), mock(CodeBaseScanner.class), mock(ServerDelegate.class),
                            "gradleVersion", "gitId");

    @Test
    public void testApplyRecordedUsage() throws Exception {
        CodeBase codeBase = new CodeBase(CodeBaseTest.buildAgentConfig("build/classes/main"));
        codeBase.readScannerResult(getResource("/customer1/app1/signatures.dat"));
        assertTrue(codeBase.hasSignature(
                "public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));

        int unrecognized = agentWorker.applyRecordedUsage(codeBase, new SignatureUsage(),
                                                          FileUtils.readUsageDataFrom(getResource("/customer1/app1/usage.dat")));

        assertThat(unrecognized, is(1));
    }

    private File getResource(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

}
