package se.crisp.codekvast.server.codekvast_server.controller;

import org.junit.Test;

public class SignatureHandlerTest {

    @Test
    public void testGetSignatures() throws Exception {
        SignatureHandler.Signatures signatures = SignatureHandler.getSignatures("user", 100);
        System.out.println("signatures.getPackages() = " + signatures.getPackages());
        System.out.println("signatures.get(0) = " + signatures.getSignatures().get(0));
    }

}
