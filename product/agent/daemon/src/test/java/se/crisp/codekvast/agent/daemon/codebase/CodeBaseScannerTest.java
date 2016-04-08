package se.crisp.codekvast.agent.daemon.codebase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.daemon.codebase.scannertest.ScannerTest1;
import se.crisp.codekvast.agent.daemon.codebase.scannertest.ScannerTest2;
import se.crisp.codekvast.agent.daemon.codebase.scannertest.ScannerTest3;
import se.crisp.codekvast.agent.daemon.codebase.scannertest.ScannerTest4;
import se.crisp.codekvast.agent.daemon.codebase.scannertest.excluded.ExcludedScannerTest5;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String TEST_CLASSES_DIR = "build/classes/test";

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private CodeBase codeBase;

    @Before
    public void before() throws Exception {
        codeBase = getCodeBase(TEST_CLASSES_DIR);
    }

    private CodeBase getCodeBase(String codeBase) {
        return new CodeBase(CollectorConfigFactory.createSampleCollectorConfig().toBuilder()
                                                  .codeBase(new File(codeBase).getAbsolutePath())
                                                  .dataPath(temporaryFolder.getRoot())
                                                  .packages(ScannerTest1.class.getPackage().getName())
                                                  .excludePackages(ExcludedScannerTest5.class.getPackage().getName())
                                                  .build());
    }

    @Test
    public void testScanCodeBaseForDirectoryWithMyClassFiles() throws URISyntaxException {
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(numClasses, is(10));

        Collection<CodeBaseEntry> entries = codeBase.getEntries();
        assertThat(entries, notNullValue());
        assertThat(entries.size(), is(17));
        assertThat(entries.stream()
                          .filter(e -> e.getSignatureStatus() == SignatureStatus.EXCLUDED_BY_PACKAGE_NAME)
                          .count(), is(1L));
        assertThat(entries.stream()
                          .filter(e -> e.getSignatureStatus() == SignatureStatus.EXCLUDED_BY_VISIBILITY)
                          .count(), is(1L));
        assertThat(entries.stream()
                          .filter(e -> e.getSignatureStatus() == SignatureStatus.EXCLUDED_SINCE_TRIVIAL)
                          .count(), is(3L));
    }

    @Test
    public void testFindBaseMethodForScannerTest2() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), of("acme."), ScannerTest2.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest2.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest3() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), of("acme."), ScannerTest3.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(2));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m1()"),
                   is("public " + ScannerTest1.class.getName() + ".m1()"));
        assertThat(codeBase.getOverriddenSignatures().get("public " + ScannerTest3.class.getName() + ".m2()"),
                   is("public " + ScannerTest2.class.getName() + ".m2()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest4() throws URISyntaxException {
        scanner.findTrackedMethods(codeBase, of("se."), of("acme."), ScannerTest4.class);
        assertThat(codeBase.getSignatures().size(), is(11));
    }

}
