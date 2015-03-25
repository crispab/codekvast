package se.crisp.codekvast.agent.main.codebase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest4;

import java.io.File;
import java.net.URISyntaxException;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static final String TEST_CLASSES_DIR = "build/classes/test";

    private final CodeBaseScanner scanner = new CodeBaseScanner();

    private CodeBase getCodeBase(String codeBase) throws URISyntaxException {
        return new CodeBase(CollectorConfig.builder()
                                           .dataPath(temporaryFolder.getRoot())
                                           .codeBase(new File(codeBase).getAbsolutePath())
                                           .packagePrefixes(ScannerTest1.class.getPackage().getName())
                                           .appName("appName")
                                           .appVersion("1.0")
                                           .tags("tags")
                                           .collectorResolutionSeconds(1)
                                           .aspectjOptions("")
                                           .methodVisibility(CollectorConfig.DEFAULT_METHOD_VISIBILITY)
                                           .build());
    }

    @Test
    public void testScanCodeBaseForDirectoryWithMyClassFiles() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(codeBase.getSignatures(), notNullValue());
        assertThat(numClasses, is(8));
        assertThat(codeBase.getSignatures().size(), is(9));
    }

    @Test
    public void testFindBaseMethodForScannerTest2() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);

        scanner.findPublicMethods(codeBase, of("se."), ScannerTest2.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().get("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2.m1()"),
                   is("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1.m1()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest3() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);

        scanner.findPublicMethods(codeBase, of("se."), ScannerTest3.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(2));
        assertThat(codeBase.getOverriddenSignatures().get("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3.m1()"),
                   is("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1.m1()"));
        assertThat(codeBase.getOverriddenSignatures().get("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3.m2()"),
                   is("public se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2.m2()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest4() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);

        scanner.findPublicMethods(codeBase, of("se."), ScannerTest4.class);

        assertThat(codeBase.getSignatures().size(), is(6));
    }

}
