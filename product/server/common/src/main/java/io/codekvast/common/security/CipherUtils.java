/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.experimental.UtilityClass;

/** @author olle.hallin@crisp.se */
@UtilityClass
public class CipherUtils {

  private static final String CIPHER_INSTANCE = "AES/ECB/PKCS5Padding";

  /**
   * Encrypts a string.
   *
   * @param plainText The string to encrypt
   * @param key The UTF-8 encoded string containing the encryption key.
   * @return A Base64-encoded encrypted version of the plain text.
   * @throws CipherException When failed to encrypt.
   */
  public static String encrypt(String plainText, String key) throws CipherException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
      cipher.init(Cipher.ENCRYPT_MODE, stringToKey(key));
      byte[] encrypted = Base64.getEncoder().encode(cipher.doFinal(plainText.getBytes(UTF_8)));
      return new String(encrypted, UTF_8);
    } catch (Exception e) {
      throw new CipherException("Cannot encrypt", e);
    }
  }

  /**
   * Decrypts a string.
   *
   * @param cipherText The string to decrypt. It comes from {@link #encrypt(String, String)}.
   * @param key The UTF-8 encoded string containing the encryption key.
   * @return The plain text.
   * @throws CipherException When failed to decrypt.
   */
  public static String decrypt(String cipherText, String key) throws CipherException {
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
      cipher.init(Cipher.DECRYPT_MODE, stringToKey(key));
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText.getBytes(UTF_8)));
      return new String(decrypted, UTF_8);
    } catch (Exception e) {
      throw new CipherException("Cannot decrypt", e);
    }
  }

  private static Key stringToKey(String key) {
    return new SecretKeySpec(key.getBytes(UTF_8), "AES");
  }

  static String generateRandomKey() throws NoSuchAlgorithmException {
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    generator.init(128);
    SecretKey key = generator.generateKey();
    return new String(Base64.getEncoder().encode(key.getEncoded()), UTF_8);
  }
}
