/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.security;

/**
 * Addtional internal interface to the security service.
 *
 * @author olle.hallin@crisp.se
 */
public interface SecurityService extends CustomerIdProvider, WebappTokenProvider {

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
}
