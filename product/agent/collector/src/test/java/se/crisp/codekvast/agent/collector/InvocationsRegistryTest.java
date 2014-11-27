package se.crisp.codekvast.agent.collector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.config.SharedConfig;
import se.crisp.codekvast.agent.model.Jvm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvocationsRegistryTest {

    private static final String CUSTOMER_NAME = "Customer Name";
    private static final String APP_NAME = "Invocations Registry Test";
    private static final String APP_VERSION = "1.2.3-rc-2";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig config;
    private URI codeBaseUri;

    @Before
    public void beforeTest() throws IOException {
        codeBaseUri = temporaryFolder.newFolder("codebase").toURI();
        File dataPath = temporaryFolder.newFolder("collector");

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
                                .methodExecutionPointcut(CollectorConfig.DEFAULT_METHOD_EXECUTION_POINTCUT)
                                .build();
        //@formatter:on
        InvocationRegistry.initialize(config);
    }

    @Test
    public void testRegisterJspPageExecutionAndDumpToDisk() throws IOException {
        InvocationRegistry.instance.registerJspPageExecution("page1");

        InvocationRegistry.instance.dumpDataToDisk(1);

        File[] files = config.getSharedConfig().getDataPath().listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("customername"));

        files = files[0].listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("invocationsregistrytest"));

        files = files[0].listFiles();
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is(CollectorConfig.INVOCATIONS_BASENAME));
        assertThat(files[1].getName(), is(CollectorConfig.JVM_BASENAME));

        Jvm jvm = Jvm.readFrom(files[1]);
        assertThat(jvm.getCollectorConfig().getSharedConfig().getCustomerName(), is(CUSTOMER_NAME));
        assertThat(jvm.getCollectorConfig().getAppName(), is(APP_NAME));
        assertThat(jvm.getCollectorConfig().getAppVersion(), is(APP_VERSION));
        assertThat(jvm.getCollectorConfig().getCodeBaseUri(), is(codeBaseUri));
    }

}
