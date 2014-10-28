package se.crisp.codekvast.agent.collector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.util.CollectorConfig;
import se.crisp.codekvast.agent.util.JvmRun;
import se.crisp.codekvast.agent.util.SharedConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UsageRegistryTest {

    private static final String CUSTOMER_NAME = "Customer Name";
    private static final String APP_NAME = "Usage Registry Test";
    private static final String APP_VERSION = "1.2.3-rc-2";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig config;
    private URI codeBaseUri;
    private File dataPath;

    @Before
    public void beforeTest() throws IOException {
        codeBaseUri = temporaryFolder.newFolder("codebase").toURI();
        dataPath = temporaryFolder.newFolder("collector");

        //@formatter:off
        config = CollectorConfig.builder()
                                .sharedConfig(SharedConfig.builder()
                                                          .dataPath(dataPath)
                                                          .customerName(CUSTOMER_NAME)
                                                          .packagePrefix("se.crisp")
                                                          .build())
                                .codeBaseUri(codeBaseUri)
                                .appName(APP_NAME)
                                .appVersion(APP_VERSION)
                                .collectorResolutionSeconds(1)
                                .aspectjOptions("")
                                .build();
        //@formatter:on
        UsageRegistry.initialize(config);
    }

    @Test
    public void testRegisterJspPageExecutionAndDumpToDisk() throws IOException {
        UsageRegistry.instance.registerJspPageExecution("page1");

        UsageRegistry.instance.dumpDataToDisk(1);

        File[] files = config.getSharedConfig().getDataPath().listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("customername"));

        files = files[0].listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("usageregistrytest"));

        files = files[0].listFiles();
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is(CollectorConfig.JVM_RUN_BASENAME));
        assertThat(files[1].getName(), is(CollectorConfig.USAGE_BASENAME));

        JvmRun jvmRun = JvmRun.readFrom(files[0]);
        assertThat(jvmRun.getSharedConfig().getCustomerName(), is(CUSTOMER_NAME));
        assertThat(jvmRun.getAppName(), is(APP_NAME));
        assertThat(jvmRun.getAppVersion(), is(APP_VERSION));
        assertThat(jvmRun.getCodeBaseUri(), is(codeBaseUri));
    }

}