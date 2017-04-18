package se.crisp.codekvast.agent.lib.codebase;

import com.google.common.io.Files;
import org.junit.Test;
import se.crisp.codekvast.agent.lib.codebase.CodeBase;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class CodeBaseTest {

    private static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    private static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

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

    private CodeBase getCodeBase(String codeBase) {
        return new CodeBase(CollectorConfigFactory.createSampleCollectorConfig()
                                                  .toBuilder()
                                                  .codeBase(new File(codeBase).getAbsolutePath())
                                                  .build());
    }

    @Test
    public void testNormalizeStrangeSignatures() throws URISyntaxException, IOException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        List<String> signatures =
                Files.readLines(new File(getClass().getResource("/customer1/signatures1.dat").toURI()), Charset.forName("UTF-8"));

        boolean inStrangeSignaturesSection = false;
        for (String signature : signatures) {
            if (signature.equals(CodeBase.RAW_STRANGE_SIGNATURES_SECTION)) {
                inStrangeSignaturesSection = true;
            } else if (inStrangeSignaturesSection) {
                String normalized = codeBase.normalizeSignature(signature);
                if (normalized != null) {
                    assertThat(String.format("Could not normalize%n%n   %s%n%n result is%n%n   %s%n", signature, normalized),
                               codeBase.isStrangeSignature(normalized), is(false));
                }
            }
        }
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
