package se.crisp.codekvast.agent.main;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CodeKvastAgentMainTest {

    @InjectMocks
    private AgentWorker agentWorker;

    @Test
    public void testApplyRecordedUsage() throws Exception {
        CodeBase codeBase = new CodeBase(CodeBaseTest.buildAgentConfig("build/classes/main"));
        codeBase.readScannerResult(getResource("/customer1/app1/signatures.dat"));
        assertTrue(codeBase.hasSignature(
                "public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));

        int unrecognized = agentWorker.applyRecordedUsage(codeBase, new AppUsage(),
                                                          FileUtils.readUsageDataFrom(getResource("/customer1/app1/usage.dat")));

        assertThat(unrecognized, is(1));
    }

    private File getResource(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

}
