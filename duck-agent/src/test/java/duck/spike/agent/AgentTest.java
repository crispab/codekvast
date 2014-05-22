package duck.spike.agent;

import duck.spike.util.SensorUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class AgentTest {

    @InjectMocks
    private Agent agent;

    private String[] guiceGeneratedMethods = {
            "public int se.transmode.tnm.module.l2mgr.impl.persistence.FlowDomainFragmentLongTransactionEAO..EnhancerByGuice..969b9638." +
                    ".FastClassByGuice..96f9109e.getIndex(com.google.inject.internal.cglib.core..Signature)",
            "public int se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a..FastClassByGuice." +
                    ".2d349e96.getIndex(java.lang.Class[])",
    };

    @Test
    public void guiceGeneratedSignaturesShouldBeIgnored() {
        for (String s : guiceGeneratedMethods) {
            String sig = agent.normalizeSignature(s);
            assertThat("Guice-generated method should be ignored", sig, nullValue());
        }
    }

    @Test
    public void testNormalizeGuiceEnhancedMethod() {
        String sig = agent.normalizeSignature(
                "public final void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a" +
                        ".removeTrails(java.util.Collection)");
        assertThat(sig,
                   is("public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));
    }

    @Test
    @Ignore("Broken until Agent.normalizeSignature() is debugged")
    public void testPrepareCodeBase() throws Exception {
        Set<String> signatures = readSignatures(getResource("/customer1/app1/signatures-guice-aop-1.dat"));
        assertTrue(signatures.contains(
                "public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));

        agent.resetSignatureUsage(signatures);

        int unrecognized = agent.applyRecordedUsage(SensorUtils.readUsageFrom(getResource("/customer1/app1/usage-guice-aop-1.dat")));

        assertThat(unrecognized, is(0));
    }

    private File getResource(String resource) throws URISyntaxException {
        return new File(getClass().getResource(resource).toURI());
    }

    private Set<String> readSignatures(File file) throws IOException {
        Set<String> result = new TreeSet<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }
}
