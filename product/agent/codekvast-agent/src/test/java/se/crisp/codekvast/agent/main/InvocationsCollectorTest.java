package se.crisp.codekvast.agent.main;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence.EXACT_MATCH;
import static se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence.FOUND_IN_PARENT_CLASS;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastAgentIntegTest
public class InvocationsCollectorTest {

    private final long now = System.currentTimeMillis();
    private final String jvmUuid1 = UUID.randomUUID().toString();
    private final String jvmUuid2 = UUID.randomUUID().toString();

    @Inject
    public InvocationsCollector invocationsCollector;

    @Test
    public void testPutSignatureOnce() {
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(0));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(0));
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(1));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(0));
    }

    @Test
    public void testPutTwoSignaturesOnce() {
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(0));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(0));

        invocationsCollector.put(jvmUuid1, now - 100L, "sig1", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig2", now, EXACT_MATCH);

        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(2));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(0));
    }

    @Test
    public void testPutSignatureTwice() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(1));
    }

    @Test
    public void testPutSignatureTriceWithDifferentTimestamps() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now - 1, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now - 2, EXACT_MATCH);
        List<SignatureEntry> signatures = invocationsCollector.getNotUploadedInvocations(jvmUuid1);
        assertThat(signatures, hasSize(1));
        assertThat(signatures.get(0).getInvokedAtMillis(), is(now));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentConfidence() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, FOUND_IN_PARENT_CLASS);
        List<SignatureEntry> signatures = invocationsCollector.getNotUploadedInvocations(jvmUuid1);
        assertThat(signatures, hasSize(1));
        assertThat(signatures.get(0).getConfidence(), is(FOUND_IN_PARENT_CLASS));
    }

    @Test
    public void testClearNotUploadedSignatures() {
        invocationsCollector.put(jvmUuid1, now - 100L, "sig", now, EXACT_MATCH);
        invocationsCollector.put(jvmUuid2, now - 100L, "sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(1));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(1));

        invocationsCollector.clearNotUploadedSignatures(jvmUuid1);

        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid1), hasSize(0));
        assertThat(invocationsCollector.getNotUploadedInvocations(jvmUuid2), hasSize(1));
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
        assertThat(notUploadedInvocations, hasSize(1));
        assertThat(notUploadedInvocations.get(0).getConfidence(), is(nullValue()));
    }
}
