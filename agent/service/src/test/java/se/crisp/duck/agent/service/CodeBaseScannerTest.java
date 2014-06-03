package se.crisp.duck.agent.service;

import org.junit.Test;
import se.crisp.duck.agent.util.Configuration;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_CLASSES = "../../sample/standalone-app/build/classes/main/";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private final CodeBaseScanner scanner = new CodeBaseScanner();

    @Test
    public void testScanCodeBaseForSingleJar() throws URISyntaxException {
        Configuration config = Configuration.builder()
                                            .packagePrefix("se.crisp")
                                            .codeBaseUri(new File(SAMPLE_APP_JAR).toURI())
                                            .build();
        CodeBaseScanner.Result result = scanner.getPublicMethodSignatures(config, new CodeBase(config.getCodeBaseUri().getPath()));
        assertThat(result.signatures, notNullValue());
        assertThat(result.signatures.size(), is(8));
    }

    @Test
    public void testScanCodeBaseForDirectoryContainingMultipleJars() throws URISyntaxException {
        Configuration config = Configuration.builder()
                                            .packagePrefix("se.crisp")
                                            .codeBaseUri(new File(SAMPLE_APP_LIB).toURI())
                                            .build();
        CodeBaseScanner.Result result = scanner.getPublicMethodSignatures(config, new CodeBase(config.getCodeBaseUri().getPath()));
        assertThat(result.signatures, notNullValue());
        assertThat(result.signatures.size(), is(8));
    }

    @Test
    public void testScanCodeBaseForDirectoryWithClassFiles() {
        Configuration config = Configuration.builder()
                                            .packagePrefix("sample")
                                            .codeBaseUri(new File(SAMPLE_APP_CLASSES).toURI())
                                            .build();
        CodeBaseScanner.Result result = scanner.getPublicMethodSignatures(config, new CodeBase(config.getCodeBaseUri().getPath()));
        assertThat(result.signatures, notNullValue());
        assertThat(result.signatures.size(), is(8));
    }

    @Test
    public void testFindBaseMethodForClass2() {
        CodeBaseScanner.Result result = new CodeBaseScanner.Result();
        scanner.findPublicMethods(result, "se.", Class2.class);
        assertThat(result.signatures.size(), is(2));
        assertThat(result.overriddenSignatures.size(), is(1));
        assertThat(result.overriddenSignatures.get("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class2.m1()"),
                   is("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class1.m1()"));
    }

    @Test
    public void testFindBaseMethodForClass3() {
        CodeBaseScanner.Result result = new CodeBaseScanner.Result();
        scanner.findPublicMethods(result, "se.", Class3.class);
        assertThat(result.signatures.size(), is(3));
        assertThat(result.overriddenSignatures.size(), is(2));
        assertThat(result.overriddenSignatures.get("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class3.m1()"),
                   is("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class1.m1()"));
        assertThat(result.overriddenSignatures.get("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class3.m2()"),
                   is("public void se.crisp.duck.agent.service.CodeBaseScannerTest.Class2.m2()"));
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
