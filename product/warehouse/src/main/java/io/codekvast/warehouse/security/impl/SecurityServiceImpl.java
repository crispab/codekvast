/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.CustomerService.InteractiveActivity;
import io.codekvast.warehouse.security.SecurityService;
import io.codekvast.warehouse.security.WebappCredentials;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private static final Set<SimpleGrantedAuthority> USER_AUTHORITY = singleton(new SimpleGrantedAuthority("ROLE_USER"));

    private static final String JWT_CLAIM_CUSTOMER_NAME = "customerName";
    private static final String JWT_CLAIM_EMAIL = "email";
    private static final String JWT_CLAIM_SOURCE = "source";
    private static final String BEARER_ = "Bearer ";

    private final CodekvastSettings settings;
    private final CustomerService customerService;
    private final byte[] jwtSecret;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;

    @Inject
    public SecurityServiceImpl(CodekvastSettings settings, CustomerService customerService) throws UnsupportedEncodingException {
        this.settings = settings;
        this.customerService = customerService;
        String secret = settings.getWebappJwtSecret();
        if (secret == null) {
            secret = "";
        }
        this.jwtSecret = secret.getBytes("UTF-8");
    }

    @Override
    public Long getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? settings.getDemoCustomerId() : (Long) authentication.getPrincipal();
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
    public String createWebappToken(Long customerId, WebappCredentials credentials) {
        return settings.isDemoMode()
            ? null
            : Jwts.builder()
                  .setId(credentials.getExternalId())
                  .setSubject(Long.toString(customerId))
                  .setIssuedAt(new Date())
                  .setExpiration(Date.from(Instant.now().plusSeconds(settings.getWebappJwtExpirationSeconds())))
                  .claim(JWT_CLAIM_CUSTOMER_NAME, credentials.getCustomerName())
                  .claim(JWT_CLAIM_EMAIL, credentials.getEmail())
                  .claim(JWT_CLAIM_SOURCE, credentials.getSource())
                  .signWith(signatureAlgorithm, jwtSecret)
                  .compact();
    }

    private Authentication toAuthentication(String token) throws AuthenticationException {

        if (token == null || settings.isDemoMode()) {
            return null;
        }

        int pos = token.startsWith(BEARER_) ? BEARER_.length() : 0;

        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token.substring(pos));

            return new PreAuthenticatedAuthenticationToken(
                Long.valueOf(claims.getBody().getSubject()),
                WebappCredentials.builder()
                                 .externalId(claims.getBody().getId())
                                 .customerName(claims.getBody().get(JWT_CLAIM_CUSTOMER_NAME, String.class))
                                 .email(claims.getBody().get(JWT_CLAIM_EMAIL, String.class))
                                 .source(claims.getBody().get(JWT_CLAIM_SOURCE, String.class))
                                 .build(),
                USER_AUTHORITY);
        } catch (Exception e) {
            logger.debug("Failed to authenticate token: " + e);
            return null;
        }
    }

    @Override
    public String renewWebappToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof PreAuthenticatedAuthenticationToken) {
            logger.debug("Authenticated");
            Long customerId = (Long) auth.getPrincipal();

            //noinspection CastToConcreteClass
            WebappCredentials credentials = (WebappCredentials) auth.getCredentials();

            customerService.registerInteractiveActivity(InteractiveActivity.builder()
                                                                           .customerId(customerId)
                                                                           .email(credentials.getEmail())
                                                                           .build());

            return createWebappToken(customerId, credentials);
        }
        return null;
    }

}
