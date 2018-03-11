/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.common.security;

/**
 * Additional internal interface to the security service.
 *
 * @author olle.hallin@crisp.se
 */
public interface SecurityService extends CustomerIdProvider {

    String USER_ROLE = "USER";

    /**
     * Used for converting a successful login to a webapp JWT token.
     *
     * @param customerId  The internal customerId
     * @param credentials The credentials to convert to a token
     * @return A (JWT) token to use when launching the webapp.
     */
    String createWebappToken(Long customerId, WebappCredentials credentials);

    /**
     * Authenticates a token attached to an incoming request.
     * If successful, the SecurityContextHolder is updated so that the rest of the request runs with a
     * known principal.
     *
     * If failure, the SecurityContextHolder is cleared so that Spring Security will return HTTP status 401.
     *
     * @param token The (JWT) token received from the webapp.
     */
    void authenticateToken(String token);

    /**
     * Should be invoked in a finally block after chain.doFilter(req, resp) to make sure that the authentication is not leaked to other
     * threads.
     */
    void removeAuthentication();

    /**
     * Perform a Single-Sign On using the parameters given by Heroku.
     *
     * It creates a token using the stored secret and compares it to the given token. If equal, the SSO is successful.
     * It also checks the timestamp to detect replay attacks.
     *
     * @param token            The token presented by Heroku
     * @param externalId       The external (Heroku) id of the customer inside the token
     * @param email            The email inside the token
     * @param timestampSeconds The timestamp inside the token
     * @return A JWT token to use for accessing the dashboard.
     */
    String doHerokuSingleSignOn(String token, String externalId, String email, long timestampSeconds);

}
