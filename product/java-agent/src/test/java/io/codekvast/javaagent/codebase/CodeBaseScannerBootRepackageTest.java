package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfigFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CodeBaseScannerBootRepackageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String CODE_BASE_DIR = "src/test/resources/sample-boot-repackaged";

    private final CodeBaseScanner scanner = new CodeBaseScanner();
    private CodeBase codeBase;

    @Before
    public void before() throws Exception {
        codeBase = new CodeBase(AgentConfigFactory.createSampleAgentConfig().toBuilder()
                                                  .codeBase(new File(CODE_BASE_DIR).getAbsolutePath())
                                                  .packages("sample")
                                                  .build());
    }

    @Test
    public void should_have_valid_codebase_dir() throws Exception {
        File dir = new File(CODE_BASE_DIR);
        assertThat(dir.isDirectory(), is(true));
    }

    @Test
    public void testScanCodeBaseForDirectoryWithMyClassFiles() throws URISyntaxException {
        int numClasses = scanner.scanSignatures(codeBase);
        assertThat(numClasses, is(4));
    }

}
