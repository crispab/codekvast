package se.crisp.codekvast.agent.main.codebase;

import org.junit.Test;
import se.crisp.codekvast.agent.config.CollectorConfig;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CodeBaseTest {

    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private CodeBase codeBase;

    private final String[] guiceGeneratedMethods = {
            "public int se.customer.module.l2mgr.impl.persistence.FlowDomainFragmentLongTransactionEAO..EnhancerByGuice..969b9638." +
                    ".FastClassByGuice..96f9109e.getIndex(com.google.inject.internal.cglib.core..Signature)",
            "public int se.customer.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a..FastClassByGuice." +
                    ".2d349e96.getIndex(java.lang.Class[])",
    };

    @Test
    public void guiceGeneratedSignaturesShouldBeIgnored() throws URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        for (String s : guiceGeneratedMethods) {
            String sig = codeBase.normalizeSignature(s);
            assertThat("Guice-generated method should be ignored", sig, nullValue());
        }
    }

    private CodeBase getCodeBase(String codeBase) throws URISyntaxException {
        return new CodeBase(CollectorConfig.builder()
                                           .dataPath(new File("."))
                                           .codeBase(new File(codeBase).getAbsolutePath())
                                           .packagePrefixes("se.crisp")
                                           .appName("appName")
                                           .appVersion("appVersion")
                                           .tags("tag1, tag2")
                                           .collectorResolutionSeconds(1)
                                           .aspectjOptions("")
                                           .methodExecutionPointcut(CollectorConfig.DEFAULT_METHOD_EXECUTION_POINTCUT)
                                           .build());
    }

    @Test
    public void testNormalizeGuiceEnhancedMethod() throws URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        String sig = codeBase.normalizeSignature(
                "public final void se.customer.module.l1mgr.connectivity.persistence.TrailEAO..EnhancerByGuice..a219ec4a" +
                        ".removeTrails(java.util.Collection)"
        );
        assertThat(sig,
                   is("public void se.customer.module.l1mgr.connectivity.persistence.TrailEAO.removeTrails(java.util.Collection)"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetUrlsForNullConfig() throws Exception {
        codeBase = new CodeBase(null);
    }

    @Test
    public void testGetUrlsForMissingFile() throws Exception {
        codeBase = getCodeBase("foobar");
        assertThat(codeBase.getUrls().length, is(0));
    }

    @Test
    public void testGetUrlsForDirectoryWithoutJars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase("build/classes/main");
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

    @Test
    public void testGetUrlsForDirectoryContainingJars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_LIB);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));
    }

    @Test
    public void testGetUrlsForSingleJar() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

}
