package se.crisp.codekvast.agent.main;

import org.junit.Test;
import se.crisp.codekvast.agent.util.AgentConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CodeBaseTest {

    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private CodeBase codeBase;

    private String[] guiceGeneratedMethods = {
            "public int se.transmode.tnm.module.l2mgr.impl.persistence.FlowDomainFragmentLongTransactionEAO..EnhancerByGuice..969b9638." +
                    ".FastClassByGuice..96f9109e.getIndex(com.google.inject.internal.cglib.core..Signature)",
            "public int se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a..FastClassByGuice." +
                    ".2d349e96.getIndex(java.lang.Class[])",
    };

    @Test
    public void guiceGeneratedSignaturesShouldBeIgnored() throws URISyntaxException {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI());
        for (String s : guiceGeneratedMethods) {
            String sig = codeBase.normalizeSignature(s);
            assertThat("Guice-generated method should be ignored", sig, nullValue());
        }
    }

    @Test
    public void testNormalizeGuiceEnhancedMethod() {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI());
        String sig = codeBase.normalizeSignature(
                "public final void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a" +
                        ".removeTrails(java.util.Collection)"
        );
        assertThat(sig,
                   is("public void se.transmode.tnm.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetUrlsForNullConfig() throws Exception {
        codeBase = new CodeBase(null, null);
    }

    @Test
    public void testGetUrlsForMissingFile() throws Exception {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File("foobar").toURI());
        assertThat(codeBase.getUrls().length, is(0));
    }

    @Test
    public void testGetUrlsForDirectoryWithoutJars() throws MalformedURLException, URISyntaxException {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File("build/classes/main").toURI());
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

    @Test
    public void testGetUrlsForDirectoryContainingJars() throws MalformedURLException, URISyntaxException {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_LIB).toURI());
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));
    }

    @Test
    public void testGetUrlsForSingleJar() throws MalformedURLException, URISyntaxException {
        codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI());
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

}
