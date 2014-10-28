package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SignatureUtilsTest {

    private static final String SIGNATURES1 = "/signatures1.dat";

    @Test
    public void testMinimizeSignatures() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SIGNATURES1)));
        String sig;
        int longestMinimized = 0;
        int longestSignature = 0;
        while ((sig = reader.readLine()) != null) {
            if (sig.trim().startsWith("#") || sig.trim().isEmpty()) {
                continue;
            }
            if (sig.contains("--------")) {
                break;
            }

            String minimized = SignatureUtils.minimizeSignature(sig);

            assertTrue(minimized, minimized.length() < sig.length());
            longestSignature = Math.max(longestSignature, sig.length());
            longestMinimized = Math.max(longestMinimized, minimized.length());
        }
        assertThat(longestSignature, is(1480));
        assertThat(longestMinimized, is(1468));
    }

}
