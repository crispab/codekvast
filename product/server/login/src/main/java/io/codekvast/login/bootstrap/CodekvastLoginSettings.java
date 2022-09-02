/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.login.bootstrap;

import io.codekvast.common.security.CipherException;
import io.codekvast.common.security.CipherUtils;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * Wrapper for environment properties codekvast.*
 *
 * @author olle.hallin@crisp.se
 */
@Component
@ConfigurationProperties(prefix = "codekvast.login")
@Validated
@Data
@Slf4j
@ToString(
    exclude = {
      "cipherSecret",
      "herokuApiPassword",
      "herokuApiSsoSalt",
      "herokuOAuthClientSecret",
    })
@SuppressWarnings({"ClassWithTooManyMethods", "ClassWithTooManyFields", "OverlyComplexClass"})
public class CodekvastLoginSettings {

  /** Which is the key used for encrypting Heroku OAuth tokens in the database? */
  @NotBlank private String cipherSecret;

  /** Which CODEKVAST_URL should be used by Heroku addons? */
  @NotBlank private String herokuCodekvastUrl;

  /** Where is the Heroku OAuth base URL? */
  @NotBlank private String herokuOAuthBaseUrl;

  /** What is the base URL for the Heroku API? */
  @NotBlank private String herokuApiBaseUrl;

  /** Which password is Heroku using when invoking us? */
  @NotBlank private String herokuApiPassword;

  /** Which salt does Heroku use when creating SSO tokens? */
  @NotBlank private String herokuApiSsoSalt;

  /** What OAuth client ID should we use when invoking the Heroku API? */
  @NotBlank private String herokuOAuthClientId;

  /** What OAuth client secret should we use when invoking the Heroku API? */
  @NotBlank private String herokuOAuthClientSecret;

  @PostConstruct
  public void validateCipherSecret() throws CipherException {
    String plainText = "The quick brown fox jumps over the lazy dog";
    String encrypted = CipherUtils.encrypt(plainText, cipherSecret);
    String decrypted = CipherUtils.decrypt(encrypted, cipherSecret);

    Assert.isTrue(decrypted.equals(plainText), "Invalid cipherSecret");
  }

  @PostConstruct
  public void logStartup() {
    //noinspection UseOfSystemOutOrSystemErr
    System.out.printf("%s starts%n", this);
    logger.info("{} starts", this);
  }

  @PreDestroy
  public void logShutdown() {
    //noinspection UseOfSystemOutOrSystemErr
    System.out.printf("%s shuts down%n", this);
    logger.info("{} shuts down", this);
  }
}
