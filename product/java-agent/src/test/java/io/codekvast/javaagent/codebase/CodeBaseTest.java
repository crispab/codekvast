package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v2.MethodSignature2;
import org.junit.Test;

import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class CodeBaseTest {

    private static final String SAMPLE_WEB_APP = "/sample-web-app";
    private static final String SAMPLE_APP_LIB = "/sample-web-app/WEB-INF/lib";
    private static final String SAMPLE_CLASSES_DIR = SAMPLE_WEB_APP + "/WEB-INF/classes";
    private static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";
    private static final String CODEBASE2 = "/codebase2";

    private CodeBase codeBase;

    private CodeBase getCodeBase(String... codeBases) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        for (String s : codeBases) {
            URL resource = getClass().getResource(s);
            if (resource != null) {
                String path = resource.getPath();
                sb.append(delimiter).append(path);
                delimiter = ", ";
            }
        }

        return new CodeBase(AgentConfigFactory.createSampleAgentConfig()
                                              .toBuilder()
                                              .codeBase(sb.toString())
                                              .packages("sample")
                                              .build());
    }

    @Test(expected = NullPointerException.class)
    public void should_handle_null_codeBase() {
        codeBase = new CodeBase(null);
    }

    @Test
    public void should_handle_missing_codeBase() {
        // when
        codeBase = getCodeBase("foobar");

        // then
        assertThat(codeBase.getUrls().length, is(0));
    }

    @Test
    public void should_handle_dir_containing_classes_but_no_jars() {
        // when
        codeBase = getCodeBase(SAMPLE_CLASSES_DIR);

        // then
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
        assertTrue(codeBase.getUrls()[0].getPath().endsWith("/"));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(1));
        assertThat(fingerprint.getNumJarFiles(), is(0));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(1));
        assertThat(codeBase.getSignatures(), hasSize(2));
        assertThatCodeBaseContains(codeBase, "InClassesOnly");
        assertThatCodeBaseNotContains(codeBase, "SampleApp");
        assertThatCodeBaseNotContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_directory_containing_only_jars() {
        // when
        codeBase = getCodeBase(SAMPLE_APP_LIB);

        // then
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(0));
        assertThat(fingerprint.getNumJarFiles(), is(3));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(14));
        assertThat(codeBase.getSignatures(), hasSize(37));
        assertThatCodeBaseNotContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_directories_containing_classes_and_jars() {
        // when
        codeBase = getCodeBase(SAMPLE_CLASSES_DIR, SAMPLE_APP_LIB);

        // then
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(4));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(1));
        assertThat(fingerprint.getNumJarFiles(), is(3));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(15));
        assertThat(codeBase.getSignatures(), hasSize(39));
        assertThatCodeBaseContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_typical_webapp() {
        // when
        codeBase = getCodeBase(SAMPLE_WEB_APP);

        // then
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(4));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(1));
        assertThat(fingerprint.getNumJarFiles(), is(3));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(15));

        assertThat(codeBase.getSignatures(), hasSize(39));
        assertThatCodeBaseContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_typical_webapp_WEB_INF() {
        codeBase = getCodeBase(SAMPLE_WEB_APP + "/WEB-INF");
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(4));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(1));
        assertThat(fingerprint.getNumJarFiles(), is(3));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(15));
        assertThat(codeBase.getSignatures(), hasSize(39));
        assertThatCodeBaseContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_typical_webapp_WEB_INF_with_extra_codeBase() {
        codeBase = getCodeBase(SAMPLE_WEB_APP + "/WEB-INF", CODEBASE2);

        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(5));

        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(1));
        assertThat(fingerprint.getNumJarFiles(), is(4));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(15));
        assertThat(codeBase.getSignatures(), hasSize(39));
        assertThatCodeBaseContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseContains(codeBase, "sample.lib");
    }

    @Test
    public void should_handle_single_jar() {
        // when
        codeBase = getCodeBase(SAMPLE_APP_JAR);

        // then
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));

        // when
        int scannedClasses = new CodeBaseScanner().scanSignatures(codeBase);

        // then
        assertThat(scannedClasses, is(6));
        CodeBaseFingerprint fingerprint = codeBase.getFingerprint();
        assertThat(fingerprint.getNumClassFiles(), is(0));
        assertThat(fingerprint.getNumJarFiles(), is(1));

        assertThat(codeBase.getSignatures(), hasSize(21));
        assertThatCodeBaseNotContains(codeBase, "InClassesOnly");
        assertThatCodeBaseContains(codeBase, "SampleApp");
        assertThatCodeBaseNotContains(codeBase, "org.slf4j");
    }

    private void assertThatCodeBaseContains(CodeBase codeBase, String signature) {
        for (MethodSignature2 sig : codeBase.getSignatures()) {
            if (sig.getAspectjString().contains(signature)) {
                return;
            }
        }
        fail("Missing signature: " + signature);
    }

    private void assertThatCodeBaseNotContains(CodeBase codeBase, String signature) {
        for (MethodSignature2 sig : codeBase.getSignatures()) {
            if (sig.getAspectjString().contains(signature)) {
                fail("Unexpected signature: " + sig.getAspectjString());
            }
        }
    }

}
