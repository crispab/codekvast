package se.crisp.codekvast.agent.main.codebase;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.config.SharedConfig;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2;
import se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerTest {

    public static final String TEST_CLASSES_DIR = "build/classes/test";

    private final CodeBaseScanner scanner = new CodeBaseScanner();

    private CodeBase getCodeBase(String codeBase) throws URISyntaxException {
        return new CodeBase(CollectorConfig.builder()
                                           .sharedConfig(SharedConfig.builder().dataPath(new File(".")).build())
                                           .codeBaseUri(new File(codeBase).toURI())
                                           .customerName("customerName")
                                           .packagePrefixes(ScannerTest1.class.getPackage().getName())
                                           .appName("appName")
                                           .appVersion("1.0")
                                           .collectorResolutionSeconds(1)
                                           .aspectjOptions("")
                                           .methodExecutionPointcut(CollectorConfig.DEFAULT_METHOD_EXECUTION_POINTCUT)
                                           .build());
    }

    @Test
    public void testScanCodeBaseForDirectoryWithMyClassFiles() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);
        scanner.getPublicMethodSignatures(codeBase);
        assertThat(codeBase.getSignatures(), notNullValue());
        assertThat(codeBase.getNumClasses(), is(4));
        assertThat(codeBase.getSignatures().size(), is(7));
    }

    @Test
    public void testFindBaseMethodForScannerTest2() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);

        scanner.findPublicMethods(codeBase, ImmutableSet.of("se."), ScannerTest2.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().get("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2.m1()"),
                   is("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1.m1()"));
    }

    @Test
    public void testFindBaseMethodForScannerTest3() throws URISyntaxException {
        CodeBase codeBase = getCodeBase(TEST_CLASSES_DIR);

        scanner.findPublicMethods(codeBase, ImmutableSet.of("se."), ScannerTest3.class);

        assertThat(codeBase.getSignatures().size(), is(1));
        assertThat(codeBase.getOverriddenSignatures().size(), is(2));
        assertThat(codeBase.getOverriddenSignatures().get("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3.m1()"),
                   is("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest1.m1()"));
        assertThat(codeBase.getOverriddenSignatures().get("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest3.m2()"),
                   is("se.crisp.codekvast.agent.main.codebase.scannertest.ScannerTest2.m2()"));
    }

}
