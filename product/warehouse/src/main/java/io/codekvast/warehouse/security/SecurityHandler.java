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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;

/**
 * @author olle.hallin@crisp.se
 */
@Component
public class SecurityHandler {
    public static final String AUTH_TOKEN_COOKIE = "authToken";

    private static long DEMO_CUSTOMER_ID = 1L;

    public Long getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? DEMO_CUSTOMER_ID : (Long) authentication.getPrincipal();
    }

    public Cookie createAuthTokenCookie(String jwt, boolean secure) {
        Cookie cookie = new Cookie(SecurityHandler.AUTH_TOKEN_COOKIE, jwt);
        cookie.setHttpOnly(false);
        cookie.setSecure(secure);
        return cookie;
    }

    String makeJwtToken(Long customerId, String email) {
        // TODO: Make a proper JWT token
        return String.format("%d:%s", customerId, email);
    }

    public String refreshJwtToken(String jwt) {
        // TODO: extend TTL on the token
        return jwt;
    }
}
