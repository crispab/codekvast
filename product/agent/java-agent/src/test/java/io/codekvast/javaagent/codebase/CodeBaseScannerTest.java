package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.codebase.scannertest.ScannerTest1;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest2;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest3;
import io.codekvast.javaagent.codebase.scannertest.ScannerTest4;
import io.codekvast.javaagent.codebase.scannertest.excluded.ExcludedScannerTest5;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v2.CodeBaseEntry2;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String TEST_CLASSES_DIR = "build/classes/java/test";
    private static final String SPRING_BOOT_EXECUTABLE_JAR_DIR = "src/test/resources/sample-spring-boot-executable-jar";

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private CodeBase codeBase;

    @Before
    public void beforeTest() {
        codeBase = new CodeBase(AgentConfigFactory
                                    .createSampleAgentConfig().toBuilder()
                                    .codeBase(new File(TEST_CLASSES_DIR).getAbsolutePath())
                                    .packages(ScannerTest1.class.getPackage().getName())
                                    .excludePackages(ExcludedScannerTest5.class.getPackage().getName())
                                    .build());
    }

    @Test
    public void should_handle_exploded_classes_dir() {
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(numClasses, is(9));

        Collection<CodeBaseEntry2> entries = codeBase.getEntries();
        assertThat(entries, notNullValue());
        for (CodeBaseEntry2 entry : entries) {
            assertThat(entry.getSignature(), not(containsString("wait()")));
        }

        assertThat(entries.size(), is(25));
    }

    @Test
    public void should_find_base_methods_of_ScannerTest2() {
        scanner.findMethods(codeBase, ScannerTest2.class, codeBase.getConfig().getNormalizedPackages());
        assertThat(codeBase.getSignatures().size(), is(1));
    }

    @Test
    public void should_find_base_methods_of_ScannerTest3() {
        scanner.findMethods(codeBase, ScannerTest3.class, codeBase.getConfig().getNormalizedPackages());

        assertThat(codeBase.getSignatures().size(), is(1));
    }

    @Test
    public void should_find_base_methods_of_ScannerTest4() {
        scanner.findMethods(codeBase, ScannerTest4.class, codeBase.getConfig().getNormalizedPackages());
        assertThat(codeBase.getSignatures().size(), is(11));
    }

    @Test
    public void should_find_constructors_of_ScannerTest4() {
        scanner.findConstructors(codeBase, ScannerTest4.class);
        assertThat(codeBase.getSignatures().size(), is(3));
    }

    @Test
    public void should_handle_spring_boot_executable_jar() {
        int numClasses = scanner.scanSignatures(new CodeBase(AgentConfigFactory
                                                                 .createSampleAgentConfig().toBuilder()
                                                                 .codeBase(new File(SPRING_BOOT_EXECUTABLE_JAR_DIR).getAbsolutePath())
                                                                 .packages("sample.springboot, sample.lib")
                                                                 .build()));
        assertThat(numClasses, is(4 + 7 + 1));
    }

    @Test
    @Ignore("Default disabled")
    public void stability_test() {
        for (int i = 0; i < 10_000; i++) {
            System.out.printf("Stability test #%05d%n", i);
            should_handle_exploded_classes_dir();
            should_handle_spring_boot_executable_jar();
        }
    }
}
