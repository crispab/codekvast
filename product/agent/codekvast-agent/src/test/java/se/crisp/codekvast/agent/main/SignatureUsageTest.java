package se.crisp.codekvast.agent.main;

import org.junit.Test;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static se.crisp.codekvast.server.agent.model.v1.UsageConfidence.EXACT_MATCH;
import static se.crisp.codekvast.server.agent.model.v1.UsageConfidence.FOUND_IN_PARENT_CLASS;

public class SignatureUsageTest {

    private final long now = System.currentTimeMillis();
    private final SignatureUsage signatureUsage = new SignatureUsage();

    @Test
    public void testPutSignatureOnce() {
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
        signatureUsage.put("sig", now, EXACT_MATCH);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testPutTwoSignaturesOnce() {
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
        signatureUsage.put("sig1", now, EXACT_MATCH);
        signatureUsage.put("sig2", now, EXACT_MATCH);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(2));
    }

    @Test
    public void testPutSignatureTwice() {
        signatureUsage.put("sig", now, EXACT_MATCH);
        signatureUsage.put("sig", now, EXACT_MATCH);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testPutSignatureTriceWithDifferentTimestamps() {
        signatureUsage.put("sig", now - 1, EXACT_MATCH);
        signatureUsage.put("sig", now, EXACT_MATCH);
        signatureUsage.put("sig", now - 2, EXACT_MATCH);
        Set<UsageDataEntry> signatures = signatureUsage.getNotUploadedSignatures();
        assertThat(signatures, hasSize(1));
        assertThat(signatures.iterator().next().getUsedAtMillis(), is(now));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentConfidence() {
        signatureUsage.put("sig", now, EXACT_MATCH);
        signatureUsage.put("sig", now, FOUND_IN_PARENT_CLASS);
        Set<UsageDataEntry> signatures = signatureUsage.getNotUploadedSignatures();
        assertThat(signatures, hasSize(1));
    }

    @Test
    public void testClearNotUploadedSignatures() {
        signatureUsage.put("sig", now, EXACT_MATCH);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
        signatureUsage.clearNotUploadedSignatures();
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
    }
}
