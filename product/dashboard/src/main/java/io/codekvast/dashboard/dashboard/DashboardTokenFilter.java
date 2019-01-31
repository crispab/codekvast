/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.dashboard.dashboard;

import io.codekvast.common.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This is a Spring Security filter that picks up and validates a JWT contained in either the
 * Authorization header or the sessionToken cookie.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardTokenFilter extends OncePerRequestFilter {

    private final SecurityService securityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        securityService.authenticateToken(getSessionToken(request));
        try {
            chain.doFilter(request, response);
        } finally {
            securityService.removeAuthentication();
        }
    }

    private String getSessionToken(HttpServletRequest request) {
        String token = getTokenFromCookie(request, CookieNames.SESSION_TOKEN);
        if (token != null) {
            logger.debug("Found sessionToken in cookie");
        } else {
            token = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (token != null) {
                logger.debug("Found sessionToken in header");
            }
        }
        return token;
    }

    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
