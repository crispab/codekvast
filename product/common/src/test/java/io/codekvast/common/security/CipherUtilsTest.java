package io.codekvast.common.security;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class CipherUtilsTest {

    private static final String KEY1 = "DAMfPsIPAjDL3xlHvakaXA==";

    private static final String PLAIN_TEXT1 = "Flygande beckasiner söka hwila på mjuka tufvor";
    private static final String CIPHER_TEXT1 = "SE0+xrapq/lp3ploXRVWF1CDMYry6CXs7o11VmCNOeMApK1fHBE9W3Ce7PteBhLGQrNYee2eI488iNoMEfV7Sg==";

    private static final String PLAIN_TEXT2 =
        "Elit mus porttitor nisi, sagittis dapibus non, ultricies? Lectus phasellus quis diam mid porttitor scelerisque dignissim ac. " +
            "Ultrices amet urna. Scelerisque ut turpis, egestas dolor? Purus eros, integer scelerisque a habitasse, ac ac augue mattis " +
            "elit sed eros integer.";

    private static final String CIPHER_TEXT2 =
        "nYenna0HeVwZ628UrKQA5Dv5aWeAX4/OvJVxOjBsrVy7vGWMhOryg9YtTiA/CoMxIo2QAq0V8+33oRwfbbIzxehyRvvSQzfIazIDz1" +
            "+cJcF3zNKnnDiCdOPnS59BVbl38YvYzCOt/nTa3pnbvmKP9WZ8maqgK6fcn1eja5t9dbDpUJuqeGlflGWyNThGhqz8aCYUkF/CNodtOORfk+ua2Y3LW09VdEh" +
            "/6WL3XmLXAdy4trjlTKDHJdkjMB5oJDsLQL+VPt72MWVfcjb0z/hYuV2qhlepj6Hjtlm77uwvISknvSGnZFZuHN+bZLdNrIGhX8I" +
            "+aHDCPyCEc018Zngwq7zMU3AjzQ61YPXb3I6EoWo=";

    @Test
    public void should_encrypt1() throws CipherException {
        assertThat(CipherUtils.encrypt(PLAIN_TEXT1, KEY1), is(CIPHER_TEXT1));
    }

    @Test
    public void should_encrypt2() throws CipherException {
        assertThat(CipherUtils.encrypt(PLAIN_TEXT2, KEY1), is(CIPHER_TEXT2));
    }

    @Test
    public void should_decrypt1() throws CipherException {
        assertThat(CipherUtils.decrypt(CIPHER_TEXT1, KEY1), is(PLAIN_TEXT1));
    }

    @Test
    public void should_decrypt2() throws CipherException {
        assertThat(CipherUtils.decrypt(CIPHER_TEXT2, KEY1), is(PLAIN_TEXT2));
    }

    @Test
    public void should_encrypt_decrypt_with_various_keys() throws CipherException, UnsupportedEncodingException, NoSuchAlgorithmException {
        for (int i = 0; i < 10; i++) {
            String key = CipherUtils.generateRandomKey();
            System.out.println(key);
            String encrypted = CipherUtils.encrypt(PLAIN_TEXT1, key);
            assertThat(CipherUtils.decrypt(encrypted, key), is(PLAIN_TEXT1));
        }
    }
}