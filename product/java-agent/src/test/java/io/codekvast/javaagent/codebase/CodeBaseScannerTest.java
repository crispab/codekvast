package io.codekvast.javaagent.codebase;

import com.google.common.collect.ImmutableSet;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest1;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest2;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest3;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest4;
import io.codekvast.javaagent.codebase.scannertest.excluded.ExcludedScannerTest5;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v1.CodeBaseEntry;
import io.codekvast.javaagent.model.v1.SignatureStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String TEST_CLASSES_DIR = "build/classes/test";
    private static final String SPRING_BOOT_EXECUTABLE_JAR_DIR = "src/test/resources/sample-spring-boot-executable-jar";

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private CodeBase codeBase;

    @Before
    public void beforeTest() throws Exception {
        codeBase = new CodeBase(AgentConfigFactory
                                    .createSampleAgentConfig().toBuilder()
                                    .codeBase(new File(TEST_CLASSES_DIR).getAbsolutePath())
                                    .packages(ScannerTest1.class.getPackage().getName())
                                    .excludePackages(ExcludedScannerTest5.class.getPackage().getName())
                                    .build());
    }

    @Test
    public void should_handle_exploded_classes_dir() throws URISyntaxException {
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(numClasses, is(9));

        Collection<CodeBaseEntry> entries = codeBase.getEntries();
        assertThat(entries, notNullValue());
        assertThat(entries.size(), is(25));
        assertThat(countBySignatureStatus(entries, SignatureStatus.EXCLUDED_BY_PACKAGE_NAME), is(1));
        assertThat(countBySignatureStatus(entries, SignatureStatus.EXCLUDED_BY_VISIBILITY), is(2));
        assertThat(countBySignatureStatus(entries, SignatureStatus.EXCLUDED_SINCE_TRIVIAL), is(3));
    }

    private int countBySignatureStatus(Collection<CodeBaseEntry> entries, SignatureStatus status) {
        int result = 0;
        for (CodeBaseEntry entry : entries) {
            if (entry.getSignatureStatus() == status) {
                result += 1;
            }
        }
        return result;
    }

    @Test
    public void should_find_base_methods_of_ScannerTest2() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, ImmutableSet.of("io."), ImmutableSet.of("acme."), ScannerTest2.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest2.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
    }

    @Test
    public void should_find_base_methods_of_ScannerTest3() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, ImmutableSet.of("io."), ImmutableSet.of("acme."), ScannerTest3.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(2));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m2()"),
                   is("public " + ScannerTest2.class.getName() + ".m2()"));
    }

    @Test
    public void should_find_base_methods_of_ScannerTest4() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, ImmutableSet.of("io."), ImmutableSet.of("acme."), ScannerTest4.class);
        assertThat(codeBase.getSignatures().size(), is(11));
    }

    @Test
    public void should_find_constructors_of_ScannerTest4() throws URISyntaxException {
        scanner.findTrackedConstructors(codeBase, ScannerTest4.class);
        assertThat(codeBase.getSignatures().size(), is(3));
    }

    @Test
    public void should_handle_spring_boot_executable_jar() throws Exception {
        int numClasses = scanner.scanSignatures(new CodeBase(AgentConfigFactory
                                                                 .createSampleAgentConfig().toBuilder()
                                                                 .codeBase(new File(SPRING_BOOT_EXECUTABLE_JAR_DIR).getAbsolutePath())
                                                                 .packages("sample.springboot, sample.lib")
                                                                 .build()));
        assertThat(numClasses, is(4 + 7 + 1));
    }

    @Test
    @Ignore("Default disabled")
    public void stability_test() throws Exception {
        for (int i = 0; i < 10_000; i++) {
            System.out.printf("Stability test #%05d%n", i);
            should_handle_exploded_classes_dir();
            should_handle_spring_boot_executable_jar();
        }
    }
}
