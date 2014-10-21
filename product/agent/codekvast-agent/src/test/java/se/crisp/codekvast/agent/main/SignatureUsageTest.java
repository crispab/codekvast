package se.crisp.codekvast.agent.main;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class SignatureUsageTest {

    private final long now = System.currentTimeMillis();
    private final SignatureUsage signatureUsage = new SignatureUsage();

    @Test
    public void testPutSignatureOnce() {
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
        signatureUsage.put("sig", now, 0);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testPutTwoSignaturesOnce() {
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
        signatureUsage.put("sig1", now, 0);
        signatureUsage.put("sig2", now, 0);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(2));
    }

    @Test
    public void testPutSignatureTwice() {
        signatureUsage.put("sig", now, 0);
        signatureUsage.put("sig", now, 0);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentTimestamps() {
        signatureUsage.put("sig", now, 0);
        signatureUsage.put("sig", now + 1, 0);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testPutSignatureTwiceWithDifferentConfidence() {
        signatureUsage.put("sig", now, 0);
        signatureUsage.put("sig", now, 1);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
    }

    @Test
    public void testClearNotUploadedSignatures() {
        signatureUsage.put("sig", now, 0);
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(1));
        signatureUsage.clearNotUploadedSignatures();
        assertThat(signatureUsage.getNotUploadedSignatures(), hasSize(0));
    }
}
