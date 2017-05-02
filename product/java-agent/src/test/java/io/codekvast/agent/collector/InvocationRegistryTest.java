package io.codekvast.agent.collector;

import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import io.codekvast.agent.lib.util.SignatureUtils;
import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvocationRegistryTest {

    private static final String APP_NAME = "Invocations Registry Test";
    private static final String APP_VERSION = "1.2.3-rc-2";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CollectorConfig config;
    private Signature signature1;
    private Signature signature2;

    @Before
    public void beforeTest() throws IOException, NoSuchMethodException {
        String codeBase = temporaryFolder.newFolder("codebase1").getAbsolutePath() + ", "
            + temporaryFolder.newFolder("codebase2").getAbsolutePath();
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
    public void should_handle_registrations_when_disabled() throws Exception {
        InvocationRegistry.initialize(null);
        assertThat(InvocationRegistry.instance.isNullRegistry(), is(true));
        InvocationRegistry.instance.registerMethodInvocation(signature1);
    }

    @Test
    public void should_handle_concurrent_registrations() throws Exception {
        doExtremelyConcurrentRegistrationOf(25, 1000, signature1, signature2, signature1, signature2);
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

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class TestClass {
        public void m1() {

        }

        public void m2() {

        }
    }
}
