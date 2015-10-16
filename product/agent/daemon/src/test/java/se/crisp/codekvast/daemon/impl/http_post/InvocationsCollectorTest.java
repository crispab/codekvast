package se.crisp.codekvast.daemon.impl.http_post;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.daemon.main.EmbeddedCodekvastHttpPostDaemonIntegTest;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureEntry;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence.EXACT_MATCH;
import static se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence.FOUND_IN_PARENT_CLASS;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastHttpPostDaemonIntegTest
public class InvocationsCollectorTest {

    private final long now = System.currentTimeMillis();
    private final String jvmUuid1 = UUID.randomUUID().toString();
    private final String jvmUuid2 = UUID.randomUUID().toString();

    @Inject
    public InvocationsCollector invocationsCollector;

    @Test
    public void testPutSignatureOnce() {
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(0));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(0));
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(1));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(0));
    }

    @Test
    public void testPutTwoSignaturesOnce() {
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(0));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(0));

        invocationsCollector.put(jvmUuid1, now - 100L, "sig1", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig2", now, EXACT_MATCH);

        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(2));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(0));
    }

    @Test
    public void testPutSignatureTwice() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(1));
    }

    @Test
    public void testPutSignatureTriceWithDifferentTimestamps() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now - 1, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now - 2, EXACT_MATCH);
        List<SignatureEntry> signatures = invocationsCollector.getNotUploadedInvocations(jvmUuid1);
        Assert.assertThat(signatures, Matchers.hasSize(1));
        Assert.assertThat(signatures.get(0).getInvokedAtMillis(), Matchers.is(now));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentConfidence() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, FOUND_IN_PARENT_CLASS);
        List<SignatureEntry> signatures = invocationsCollector.getNotUploadedInvocations(jvmUuid1);
        Assert.assertThat(signatures, Matchers.hasSize(1));
        Assert.assertThat(signatures.get(0).getConfidence(), Matchers.is(FOUND_IN_PARENT_CLASS));
    }

    @Test
    public void testClearNotUploadedSignatures() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid2, now - 100L, "sig", now, EXACT_MATCH);
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(1));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(1));

        invocationsCollector.clearNotUploadedSignatures(jvmUuid1);

        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), Matchers.hasSize(0));
        Assert.assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), Matchers.hasSize(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutNullSignature() throws Exception {
        invocationsCollector.put(jvmUuid1, now - 100L, null, 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutNegativeSignature() throws Exception {
        invocationsCollector.put(jvmUuid1, now, "sig", -1L, null);
    }

    @Test
    public void testPutBeforeJvmStarted() throws Exception {
        invocationsCollector.put(jvmUuid1, now, "sig", now - 1L, null);
    }

    @Test
    public void testPutZeroSignature() throws Exception {
        invocationsCollector.put(jvmUuid1, now, "sig", 0L, null);
        List<SignatureEntry> notUploadedInvocations = invocationsCollector.getNotUploadedInvocations(jvmUuid1);
        Assert.assertThat(notUploadedInvocations, Matchers.hasSize(1));
        Assert.assertThat(notUploadedInvocations.get(0).getConfidence(), Matchers.is(Matchers.nullValue()));
    }
}
