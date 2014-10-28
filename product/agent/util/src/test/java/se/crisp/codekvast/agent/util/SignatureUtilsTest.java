package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;

public class SignatureUtilsTest {

    private static final String SIGNATURES1 = "/signatures1.dat";

    @Test
    public void testMinimizeSignatures() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SIGNATURES1)));
        String sig;
        while ((sig = reader.readLine()) != null) {
            if (sig.trim().startsWith("#") || sig.trim().isEmpty()) {
                continue;
            }
            if (sig.contains("--------")) {
                break;
            }
            String minimized = SignatureUtils.minimizeSignature(sig);
            assertTrue(minimized, minimized.length() < sig.length());
        }
    }

}
