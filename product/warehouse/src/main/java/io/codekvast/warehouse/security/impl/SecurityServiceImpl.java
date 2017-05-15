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
package io.codekvast.warehouse.security.impl;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * @author olle.hallin@crisp.se
 */
@Component
public class SecurityServiceImpl implements SecurityService {

    private static final Set<SimpleGrantedAuthority> USER_ROLE = singleton(new SimpleGrantedAuthority("ROLE_USER"));

    private static long DEMO_CUSTOMER_ID = 1L;

    private final CodekvastSettings settings;

    @Inject
    public SecurityServiceImpl(CodekvastSettings settings) {
        this.settings = settings;
    }

    @Override
    public Long getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? DEMO_CUSTOMER_ID : (Long) authentication.getPrincipal();
    }

    @Override
    public void authenticateToken(String token) {
        SecurityContextHolder.getContext().setAuthentication(toAuthentication(token));
    }

    @Override
    public void removeAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public String createWebappToken(Long customerId, String email) {
        // TODO: Make a proper JWT token
        long expiresAt = System.currentTimeMillis() + settings.getWebappJwtExpirationSeconds() * 1000L;
        return String.format("%d:%d:%s", customerId, expiresAt, email);
    }

    private Authentication toAuthentication(String token) throws AuthenticationException {
        // TODO: extract from proper JWT bearer token
        if (token == null) {
            return null;
        }

        String parts[] = token.split(":");
        if (parts.length != 3) {
            return null;
        }

        try {
            Long customerId = Long.valueOf(parts[0]);
            // TODO: validate customerId

            Long expiresAtMillis = Long.valueOf(parts[1]);
            long now = System.currentTimeMillis();
            if (expiresAtMillis < now) {
                throw new CredentialsExpiredException("Token has expired");
            }

            String email = parts[2];
            return new PreAuthenticatedAuthenticationToken(customerId, email, USER_ROLE);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String renewWebappToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof PreAuthenticatedAuthenticationToken) {
            Long customerId = (Long) auth.getPrincipal();
            String email = (String) auth.getCredentials();

            return createWebappToken(customerId, email);
        }
        return null;
    }
}
