package se.crisp.codekvast.agent.codebase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.codebase.scannertest.ScannerTest1;
import se.crisp.codekvast.agent.codebase.scannertest.ScannerTest2;
import se.crisp.codekvast.agent.codebase.scannertest.ScannerTest3;
import se.crisp.codekvast.agent.codebase.scannertest.ScannerTest4;
import se.crisp.codekvast.agent.config.CollectorConfig;

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
    private CodeBase codeBase;

    @Before
    public void before() throws Exception {
        codeBase = getCodeBase(TEST_CLASSES_DIR);
    }

    private CodeBase getCodeBase(String codeBase) {
        return new CodeBase(CollectorConfig.builder()
                                           .dataPath(temporaryFolder.getRoot())
                                           .codeBase(new File(codeBase).getAbsolutePath())
                                           .packagePrefixes(ScannerTest1.class.getPackage().getName())
                                           .appName("appName")
                                           .appVersion("1.0")
                                           .tags("tags")
                                           .collectorResolutionSeconds(1)
                                           .aspectjOptions("")
                                           .methodVisibility("private")
                                           .build());
    }

    @Test
    public void testScanCodeBaseForDirectoryWithMyClassFiles() throws URISyntaxException {
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(codeBase.getSignatures(), notNullValue());
        assertThat(numClasses, is(9));
        assertThat(codeBase.getSignatures().size(), is(10));
    }

    @Test
    public void testFindBaseMethodForScannerTest2() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), ScannerTest2.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest2.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest3() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), ScannerTest3.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(2));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m2()"),
                   is("public " + ScannerTest2.class.getName() + ".m2()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest4() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), ScannerTest4.class);
        assertThat(codeBase.getSignatures().size(), is(5));
    }

}
