package io.codekvast.javaagent.collector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.InvocationRegistry;
import io.codekvast.javaagent.codebase.CodeBaseFingerprint;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.publishing.CodekvastPublishingException;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.util.SignatureUtils;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InvocationRegistryTest {

  private static final String APP_NAME = "Invocations Registry Test";
  private static final String APP_VERSION = "1.2.3-rc-2";

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Signature signature1;
  private Signature signature2;

  @Before
  public void beforeTest() throws IOException, NoSuchMethodException {
    String codeBase =
        temporaryFolder.newFolder("codebase1").getAbsolutePath()
            + ", "
            + temporaryFolder.newFolder("codebase2").getAbsolutePath();

    AgentConfig config =
        AgentConfigFactory.createSampleAgentConfig().toBuilder()
            .appName(APP_NAME)
            .appVersion(APP_VERSION)
            .codeBase(codeBase)
            .build();
    InvocationRegistry.initialize(config);
    signature1 = SignatureUtils.makeSignature(TestClass.class, TestClass.class.getMethod("m1"));
    signature2 = SignatureUtils.makeSignature(TestClass.class, TestClass.class.getMethod("m2"));
  }

  @After
  public void afterTest() {
    InvocationRegistry.initialize(null);
  }

  @Test
  public void should_handle_registrations_when_disabled() {
    InvocationRegistry.initialize(null);
    assertThat(InvocationRegistry.instance.isNullRegistry(), is(true));
    InvocationRegistry.instance.registerMethodInvocation(signature1);
  }

  @Test
  public void should_handle_concurrent_registrations() throws Exception {
    doExtremelyConcurrentRegistrationOf(25, 1000, signature1, signature2, signature1, signature2);
  }

  private void doExtremelyConcurrentRegistrationOf(
      int numThreads, final int numRegistrations, final Signature... signatures)
      throws InterruptedException {

    final CountDownLatch startingGun = new CountDownLatch(1);
    final CountDownLatch finishLine = new CountDownLatch(numThreads * signatures.length);

    for (final Signature signature : signatures) {
      for (int i = 0; i < numThreads; i++) {
        Thread t =
            new Thread(
                () -> {
                  try {
                    startingGun.await();
                    for (int j = 0; j < numRegistrations; j++) {
                      InvocationRegistry.instance.registerMethodInvocation(signature);
                    }
                  } catch (InterruptedException ignore) {
                  } finally {
                    finishLine.countDown();
                  }
                });
        t.start();
      }
    }

    Thread publisher =
        new Thread(
            () -> {
              NullInvocationDataPublisher publisher1 = new NullInvocationDataPublisher();
              try {
                startingGun.await();
                while (true) {
                  InvocationRegistry.instance.publishInvocationData(publisher1);
                }
              } catch (InterruptedException | CodekvastPublishingException ignore) {
              }
            });
    publisher.start();

    startingGun.countDown();
    finishLine.await();
    publisher.interrupt();
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public static class TestClass {
    public void m1() {}

    public void m2() {}
  }

  private static class NullInvocationDataPublisher implements InvocationDataPublisher {
    @Override
    public void setCodeBaseFingerprint(CodeBaseFingerprint fingerprint) {}

    @Override
    public CodeBaseFingerprint getCodeBaseFingerprint() {
      return null;
    }

    @Override
    public void publishInvocationData(
        long recordingIntervalStartedAtMillis, Set<String> invocations) {}

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void configure(long customerId, String keyValuePairs) {}

    @Override
    public int getSequenceNumber() {
      return 0;
    }

    @Override
    public boolean isEnabled() {
      return false;
    }
  }
}
