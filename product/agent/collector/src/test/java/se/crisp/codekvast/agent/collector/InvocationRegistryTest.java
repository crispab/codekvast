package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.config.MethodFilter;
import se.crisp.codekvast.agent.lib.io.FileSystemInvocationDataDumper;
import se.crisp.codekvast.agent.lib.model.Jvm;
import se.crisp.codekvast.agent.lib.util.SignatureUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvocationRegistryTest {

    private static final String APP_NAME = "Invocations Registry Test";
    private static final String APP_VERSION = "1.2.3-rc-2";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig config;
    private String codeBase;
    private Signature signature;

    @Before
    public void beforeTest() throws IOException, NoSuchMethodException {
        codeBase =
                temporaryFolder.newFolder("codebase1").getAbsolutePath() + ", " + temporaryFolder.newFolder("codebase2").getAbsolutePath();
        File dataPath = temporaryFolder.newFolder("collector");

        //@formatter:off
        config = CollectorConfigFactory.builder()
                                       .appName(APP_NAME)
                                       .appVersion(APP_VERSION)
                                       .codeBase(codeBase)
                                       .dataPath(dataPath)
                                       .packagePrefixes("se.crisp")
                                       .excludePackagePrefixes("")
                                       .build();
        //@formatter:on
        InvocationRegistry.initialize(config, new FileSystemInvocationDataDumper(config, CodekvastCollector.out));
        signature = SignatureUtils.makeSignature(new MethodFilter("public"), TestClass.class, TestClass.class.getMethod("m1"));
    }

    @After
    public void afterTest() throws Exception {
        InvocationRegistry.initialize(null, null);
    }

    @Test
    public void testRegisterMethodInvocationAndDumpToDisk() throws IOException {
        assertThat(InvocationRegistry.instance.isNullRegistry(), is(false));

        InvocationRegistry.instance.registerMethodInvocation(signature);

        InvocationRegistry.instance.dumpData(1);

        File[] files = config.getDataPath().listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("invocationsregistrytest"));

        files = files[0].listFiles();
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is(CollectorConfig.INVOCATIONS_BASENAME + ".00000"));
        assertThat(files[1].getName(), is(CollectorConfig.JVM_BASENAME));

        Jvm jvm = Jvm.readFrom(files[1]);
        assertThat(jvm.getCollectorConfig().getAppName(), is(APP_NAME));
        assertThat(jvm.getCollectorConfig().getAppVersion(), is(APP_VERSION));
        assertThat(jvm.getCollectorConfig().getCodeBase(), is(codeBase));
    }

    @Test
    public void testRegisterBeforeInitialize() throws Exception {
        InvocationRegistry.initialize(null, null);
        assertThat(InvocationRegistry.instance.isNullRegistry(), is(true));
        InvocationRegistry.instance.registerMethodInvocation(signature);
    }

    @SuppressWarnings("unused")
    public static class TestClass {
        public void m1() {

        }
    }
}
