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

import javax.servlet.http.Cookie;

/**
 * Additional internal interface to the security service.
 *
 * @author olle.hallin@crisp.se
 */
public interface SecurityService extends CustomerIdProvider {

    String SESSION_TOKEN_COOKIE = "sessionToken";
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
     * Creates a cookie with the name {@value SESSION_TOKEN_COOKIE}.
     *
     * @param token      The cookie value
     * @param hostHeader The value of the HTTP header "Host". Is used for setting the cookie domain. The port number (if present) is
     *                   ignored.
     * @return A httpOnly session cookie with the path '/'.
     */
    Cookie createSessionTokenCookie(String token, String hostHeader);

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
