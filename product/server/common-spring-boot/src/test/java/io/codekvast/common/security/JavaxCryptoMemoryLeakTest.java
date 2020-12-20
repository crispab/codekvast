package io.codekvast.common.security;

import io.codekvast.common.bootstrap.Workarounds;
import java.security.Provider;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
class JavaxCryptoMemoryLeakTest {

  @BeforeAll
  static void setUp() {
    new Workarounds().patchMemoryLeakIn_javax_crypto_JceSecurity();
  }

  @Test
  @Disabled("Takes very long time to run, even with -Xmx50m")
  void provoke_OutOfMemoryError() throws Exception {
    for (int i = 0; i < 1_000_000; i++) {
      Class<?> cl = Class.forName("com.sun.crypto.provider.SunJCE");
      Provider provider = (Provider) cl.getDeclaredConstructor().newInstance();
      Cipher c = Cipher.getInstance("AES", provider);
    }
  }
}
