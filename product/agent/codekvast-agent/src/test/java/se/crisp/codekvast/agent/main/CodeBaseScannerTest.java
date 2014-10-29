package se.crisp.codekvast.agent.main;

import org.junit.Test;
import se.crisp.codekvast.agent.config.AgentConfig;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    public static final String APP_NAME = "appName";
    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_CLASSES = "src/test/resources/sample-app/classes/";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private final CodeBaseScanner scanner = new CodeBaseScanner();

    @Test
    public void testScanCodeBaseForSingleJar() throws URISyntaxException {
        CodeBase codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI(), APP_NAME);
        scanner.getPublicMethodSignatures(codeBase);
        assertThat(codeBase.signatures, notNullValue());
        assertThat(codeBase.signatures.size(), is(8));
    }

    @Test
    public void testScanCodeBaseForDirectoryContainingMultipleJars() throws URISyntaxException {
        CodeBase codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_LIB).toURI(), APP_NAME);
        scanner.getPublicMethodSignatures(codeBase);
        assertThat(codeBase.signatures, notNullValue());
        assertThat(codeBase.signatures.size(), is(8));
    }

    @Test
    public void testScanCodeBaseForDirectoryWithClassFiles() {
        CodeBase codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_CLASSES).toURI(), APP_NAME);
        scanner.getPublicMethodSignatures(codeBase);
        assertThat(codeBase.signatures, notNullValue());
        assertThat(codeBase.signatures.size(), is(8));
    }

    @Test
    public void testFindBaseMethodForClass2() {
        CodeBase codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI(), APP_NAME);

        scanner.findPublicMethods(codeBase, "se.", Class2.class);

        assertThat(codeBase.signatures.size(), is(2));
        assertThat(codeBase.overriddenSignatures.size(), is(1));
        assertThat(codeBase.overriddenSignatures.get("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class2.m1()"),
                   is("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class1.m1()"));
    }

    @Test
    public void testFindBaseMethodForClass3() {
        CodeBase codeBase = new CodeBase(AgentConfig.createSampleAgentConfig(), new File(SAMPLE_APP_JAR).toURI(), APP_NAME);

        scanner.findPublicMethods(codeBase, "se.", Class3.class);

        assertThat(codeBase.signatures.size(), is(3));
        assertThat(codeBase.overriddenSignatures.size(), is(2));
        assertThat(codeBase.overriddenSignatures.get("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class3.m1()"),
                   is("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class1.m1()"));
        assertThat(codeBase.overriddenSignatures.get("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class3.m2()"),
                   is("se.crisp.codekvast.agent.main.CodeBaseScannerTest.Class2.m2()"));
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Class1 {
        public void m1() {
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class Class2 extends Class1 {
        public void m2() {
        }
    }

    @SuppressWarnings({"ClassTooDeepInInheritanceTree", "UnusedDeclaration"})
    private static class Class3 extends Class2 {
        public void m3() {
        }
    }
}
