package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;
import se.crisp.codekvast.agent.lib.model.Invocation;
import se.crisp.codekvast.agent.lib.model.Jvm;
import se.crisp.codekvast.agent.lib.util.FileUtils;
import se.crisp.codekvast.agent.lib.util.SignatureUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvocationRegistryTest {

    private static final String APP_NAME = "Invocations Registry Test";
    private static final String APP_VERSION = "1.2.3-rc-2";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig config;
    private String codeBase;
    private Signature signature1;
    private Signature signature2;

    @Before
    public void beforeTest() throws IOException, NoSuchMethodException {
        codeBase =
                temporaryFolder.newFolder("codebase1").getAbsolutePath() + ", " + temporaryFolder.newFolder("codebase2").getAbsolutePath();
        File dataPath = temporaryFolder.newFolder(".collector");

        //@formatter:off
        config = CollectorConfigFactory.createSampleCollectorConfig().toBuilder()
                                       .appName(APP_NAME)
                                       .appVersion(APP_VERSION)
                                       .codeBase(codeBase)
                                       .dataPath(dataPath)
                                       .build();
        //@formatter:on
        InvocationRegistry.initialize(config);
        signature1 = SignatureUtils.makeSignature(TestClass.class, TestClass.class.getMethod("m1"));
        signature2 = SignatureUtils.makeSignature(TestClass.class, TestClass.class.getMethod("m2"));
    }

    @After
    public void afterTest() throws Exception {
        InvocationRegistry.initialize(null);
    }

    @Test
    public void testRegisterMethodInvocationAndPublishToDisk() throws IOException, InterruptedException {
        assertThat(InvocationRegistry.instance.isNullRegistry(), is(false));

        doExtremelyConcurrentRegistrationOf(10, 10, signature1, signature2);

        InvocationRegistry.instance.publishData(1);

        File[] files = config.getDataPath().listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is(APP_NAME.toLowerCase().replace(" ", "")));

        files = files[0].listFiles();
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is(CollectorConfig.INVOCATIONS_BASENAME + ".00000"));
        assertThat(files[1].getName(), is(CollectorConfig.JVM_BASENAME));

        List<Invocation> invocations = FileUtils.readInvocationDataFrom(files[0]);
        assertThat(invocations.size(), is(2));
        assertThat(invocations.get(0).getSignature(),
                   is("public se.crisp.codekvast.agent.collector.InvocationRegistryTest.TestClass.m1()"));
        assertThat(invocations.get(1).getSignature(),
                   is("public se.crisp.codekvast.agent.collector.InvocationRegistryTest.TestClass.m2()"));

        Jvm jvm = Jvm.readFrom(files[1]);
        assertThat(jvm.getCollectorConfig().getAppName(), is(APP_NAME));
        assertThat(jvm.getCollectorConfig().getAppVersion(), is(APP_VERSION));
        assertThat(jvm.getCollectorConfig().getCodeBase(), is(codeBase));
    }

    private void doExtremelyConcurrentRegistrationOf(int numThreads, final int numRegistrations, final Signature... signatures)
            throws InterruptedException {

        final CountDownLatch startingGun = new CountDownLatch(1);
        final CountDownLatch finishLine = new CountDownLatch(numThreads * signatures.length);

        for (final Signature signature : signatures) {
            for (int i = 0; i < numThreads; i++) {
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            startingGun.await();
                            for (int j = 0; j < numRegistrations; j++) {
                                InvocationRegistry.instance.registerMethodInvocation(signature);
                            }
                        } catch (InterruptedException ignore) {
                        } finally {
                            finishLine.countDown();
                        }

                    }
                });
                t.start();
            }
        }
        startingGun.countDown();
        finishLine.await();
    }

    @Test
    public void testRegisterBeforeInitialize() throws Exception {
        InvocationRegistry.initialize(null);
        assertThat(InvocationRegistry.instance.isNullRegistry(), is(true));
        InvocationRegistry.instance.registerMethodInvocation(signature1);
    }

    @SuppressWarnings("unused")
    public static class TestClass {
        public void m1() {

        }

        public void m2() {

        }
    }
}
