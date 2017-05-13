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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

import static java.util.Collections.*;

/**
 * @author olle.hallin@crisp.se
 */
@Component
public class SecurityHandler {
    public static final String AUTH_TOKEN_COOKIE = "authToken";
    private static final long TTL_SECONDS = 1800L;
    private static final Set<SimpleGrantedAuthority> USER_ROLE = singleton(new SimpleGrantedAuthority("ROLE_USER"));
    private static long DEMO_CUSTOMER_ID = 1L;

    public Long getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? DEMO_CUSTOMER_ID : (Long) authentication.getPrincipal();
    }

    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String token = getAuthToken(request);
        attachAuthToken(response, refreshToken(token), request.isSecure());
    }

    String createToken(Long customerId, String email) {
        return createToken(customerId, TTL_SECONDS, email);
    }

    void attachAuthToken(HttpServletResponse response, String token, boolean secure) {
        response.addCookie(createAuthTokenCookie(token, secure));
    }

    void authenticate(HttpServletRequest request) {
        String token = getAuthToken(request);
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(token));
    }

    void removeAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private String createToken(Long customerId, Long ttlSeconds, String email) {
        // TODO: Make a proper JWT token
        return String.format("%d:%d:%s", customerId, ttlSeconds, email);
    }

    private String getAuthToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(SecurityHandler.AUTH_TOKEN_COOKIE)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String refreshToken(String token) {
        // TODO: extend TTL on the JWT token
        if (token == null) {
            return null;
        }
        String parts[] = token.split(":");
        if (parts.length != 3) {
            return null;
        }
        try {
            Long customerId = Long.valueOf(parts[0]);
            Long ttlSeconds = Long.valueOf(parts[1]);
            String email = parts[2];
            return createToken(customerId, ttlSeconds + 300, email);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Cookie createAuthTokenCookie(String jwt, boolean secure) {
        Cookie cookie = new Cookie(SecurityHandler.AUTH_TOKEN_COOKIE, jwt);
        cookie.setHttpOnly(false);
        cookie.setSecure(secure);
        return cookie;
    }

    private Authentication toAuthentication(String token) {
        // TODO: extract from proper JWT token
        if (token == null) {
            return null;
        }

        String parts[] = token.split(":");
        if (parts.length != 3) {
            return null;
        }

        try {
            Long customerId = Long.valueOf(parts[0]);
            String email = parts[2];
            return new PreAuthenticatedAuthenticationToken(customerId, email, USER_ROLE);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
