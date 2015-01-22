package se.crisp.codekvast.agent.main;

import org.junit.Test;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence.EXACT_MATCH;
import static se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence.FOUND_IN_PARENT_CLASS;

public class SignatureInvocationTest {

    private final long now = System.currentTimeMillis();
    private final InvocationsCollector invocationsCollector = new InvocationsCollector();

    @Test
    public void testPutSignatureOnce() {
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(0));
        invocationsCollector.put("sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(1));
    }

    @Test
    public void testPutTwoSignaturesOnce() {
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(0));
        invocationsCollector.put("sig1", now, EXACT_MATCH);
        invocationsCollector.put("sig2", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(2));
    }

    @Test
    public void testPutSignatureTwice() {
        invocationsCollector.put("sig", now, EXACT_MATCH);
        invocationsCollector.put("sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(1));
    }

    @Test
    public void testPutSignatureTriceWithDifferentTimestamps() {
        invocationsCollector.put("sig", now - 1, EXACT_MATCH);
        invocationsCollector.put("sig", now, EXACT_MATCH);
        invocationsCollector.put("sig", now - 2, EXACT_MATCH);
        Set<InvocationEntry> signatures = invocationsCollector.getNotUploadedInvocations();
        assertThat(signatures, hasSize(1));
        assertThat(signatures.iterator().next().getInvokedAtMillis(), is(now));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentConfidence() {
        invocationsCollector.put("sig", now, EXACT_MATCH);
        invocationsCollector.put("sig", now, FOUND_IN_PARENT_CLASS);
        Set<InvocationEntry> signatures = invocationsCollector.getNotUploadedInvocations();
        assertThat(signatures, hasSize(1));
    }

    @Test
    public void testClearNotUploadedSignatures() {
        invocationsCollector.put("sig", now, EXACT_MATCH);
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(1));
        invocationsCollector.clearNotUploadedSignatures();
        assertThat(invocationsCollector.getNotUploadedInvocations(), hasSize(0));
    }
}
