package se.crisp.codekvast.server.codekvast_server.messagehandler;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SignatureHandlerTest {

    private static final long now = System.currentTimeMillis();

    @Test
    public void testGetSignatures() throws Exception {
        List<SignatureHandler.Signature> signatures = SignatureHandler.getSignatures("user");
        System.out.println("signatures.get(0) = " + signatures.get(0));
    }

    @Test
    public void testGetAgeInMinutes() throws Exception {
        assertThat(SignatureHandler.getAge(now, now - 6 * 60 * 1000L - 10), is("6 min"));
    }

    @Test
    public void testGetAgeInHours() throws Exception {
        assertThat(SignatureHandler.getAge(now, now - 6 * 60 * 60 * 1000L - 10), is("6 hours"));
    }

    @Test
    public void testGetAgeInDays() throws Exception {
        assertThat(SignatureHandler.getAge(now, now - 6 * 24 * 60 * 60 * 1000L - 10), is("6 days"));
    }

    @Test
    public void testGetAgeInWeeks() throws Exception {
        assertThat(SignatureHandler.getAge(now, now - 40 * 24 * 60 * 60 * 1000L - 10), is("5 weeks"));
    }
}
