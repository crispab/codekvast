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
package io.codekvast.login.heroku;

import io.codekvast.common.security.CipherException;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import java.time.Instant;

/**
 * DAO for the heroku_details table.
 *
 * @author olle.hallin@crisp.se
 */
public interface HerokuDetailsDAO {

  /**
   * Is there already a row in heroku_details for the customer?
   *
   * @param licenseKey The license key for the customer.
   * @return true if there is a existing row in heroku_details.
   */
  boolean existsRow(String licenseKey);

  /**
   * Saves a token response in heroku_details. The tokens are encrypted before written to the
   * database.
   *
   * @param tokenResponse The token response to save.
   * @param callbackUrl The callback URL received in the {@link HerokuProvisionRequest}.
   * @param licenseKey The licenseKey for the customer.
   * @throws CipherException If the encrypting of the tokens failed.
   */
  void saveTokens(HerokuOAuthTokenResponse tokenResponse, String callbackUrl, String licenseKey)
      throws CipherException;

  String getAccessToken(Long customerId) throws CipherException;

  String getRefreshToken(Long customerId) throws CipherException;

  String getCallbackUrl(Long customerId);

  void updateAccessToken(Long customerId, String accessToken, Instant expiresAt)
      throws CipherException;

  Instant getAccessTokenExpiresAt(Long customerId);
}
