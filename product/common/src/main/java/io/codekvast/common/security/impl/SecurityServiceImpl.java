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
package io.codekvast.common.security.impl;

import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.security.SecurityService;
import io.codekvast.common.security.WebappCredentials;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.www.NonceExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singleton;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private static final Set<SimpleGrantedAuthority> USER_AUTHORITY = singleton(new SimpleGrantedAuthority("ROLE_" + USER_ROLE));

    private static final String JWT_CLAIM_EMAIL = "email";
    private static final String JWT_CLAIM_SOURCE = "source";
    private static final String BEARER_ = "Bearer ";

    private final CodekvastCommonSettings settings;
    private final CustomerService customerService;
    private final JdbcTemplate jdbcTemplate;

    private TokenFactory tokenFactory;
    private byte[] jwtSecret;

    @PostConstruct
    public void postConstruct() throws UnsupportedEncodingException {
        String secret = settings.getDashboardJwtSecret();
        if (secret == null) {
            secret = "";
        }

        this.tokenFactory = TokenFactory.builder()
                                        .jwtExpirationHours(settings.getDashboardJwtExpirationHours())
                                        .jwtSecret(secret)
                                        .build();
        this.jwtSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Long getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : (Long) authentication.getPrincipal();
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
    @Transactional(rollbackFor = Exception.class)
    public String createCodeForWebappToken(Long customerId, WebappCredentials credentials) {
        String token = tokenFactory.createWebappToken(customerId, credentials);
        String code = UUID.randomUUID().toString().replace("-", "").toLowerCase();

        jdbcTemplate.update("INSERT INTO tokens(code, token, expiresAtSeconds) VALUES(?, ?, ?)", code, token,
                            Instant.now().plusSeconds(300).getEpochSecond());
        logger.info("Inserted token with code '{}' into database", maskSecondHalf(code));
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String tradeCodeToWebappToken(String code) {
        String token;
        try {
            token = jdbcTemplate.queryForObject(
                "SELECT token FROM tokens WHERE code = ? AND expiresAtSeconds > ? FOR UPDATE ", String.class,
                code, Instant.now().getEpochSecond());
        } catch (IncorrectResultSizeDataAccessException e) {
            logger.warn("Invalid token code: {}", maskSecondHalf(code));
            return null;
        }
        int deleted = jdbcTemplate.update("DELETE FROM tokens WHERE code = ? ", code);
        if (deleted > 0) {
            logger.info("Deleted token with code '{}' from database", maskSecondHalf(code));
        } else {
            logger.error("Could node delete token with code '{}' from database", maskSecondHalf(code));
        }
        return token;
    }

    @Scheduled(initialDelay = 60_000L, fixedRate = 600_000L)
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredTokenCodes() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("Codekvast token cleaner");
        try {
            int expired = jdbcTemplate.update("DELETE FROM tokens WHERE expiresAtSeconds <= ? ", Instant.now().getEpochSecond());
            if (expired > 0) {
                logger.info("Deleted {} expired token codes", expired);
            }
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    private Authentication toAuthentication(String token) throws AuthenticationException {
        if (token == null) {
            return null;
        }

        int pos = token.startsWith(BEARER_) ? BEARER_.length() : 0;

        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token.substring(pos));

            return new PreAuthenticatedAuthenticationToken(
                Long.valueOf(claims.getBody().getId()),
                WebappCredentials.builder()
                                 .customerName((claims.getBody().getSubject()))
                                 .email(claims.getBody().get(JWT_CLAIM_EMAIL, String.class))
                                 .source(claims.getBody().get(JWT_CLAIM_SOURCE, String.class))
                                 .build(),
                USER_AUTHORITY);
        } catch (Exception e) {
            logger.debug("Failed to authenticate token: " + e);
            return null;
        }
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    String makeHerokuSsoToken(String externalId, long timestampSeconds, String salt) {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

        byte[] digest = sha1.digest(String.format("%s:%s:%d", externalId, salt, timestampSeconds).getBytes());
        return String.format("%x", new BigInteger(1, digest));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String doHerokuSingleSignOn(String token, String externalId, String email, long timestampSeconds, String salt)
        throws AuthenticationException {
        String expectedToken = makeHerokuSsoToken(externalId, timestampSeconds, salt);
        logger.debug("id={}, token={}, timestamp={}, expectedToken={}", externalId, maskSecondHalf(token), timestampSeconds,
                     maskSecondHalf(expectedToken));

        long nowSeconds = Instant.now().getEpochSecond();
        if (timestampSeconds > nowSeconds + 60) {
            throw new NonceExpiredException("Timestamp is too far into the future");
        }

        if (timestampSeconds < nowSeconds - 5 * 60) {
            throw new NonceExpiredException("Timestamp is too old");
        }

        if (!expectedToken.equals(token)) {
            throw new BadCredentialsException("Invalid token");
        }

        CustomerData customerData = customerService.getCustomerDataByExternalId(externalId);

        customerService.registerLogin(CustomerService.LoginRequest.builder()
                                                                  .customerId(customerData.getCustomerId())
                                                                  .source(CustomerService.Source.HEROKU)
                                                                  .email(email)
                                                                  .build());

        return createCodeForWebappToken(
            customerData.getCustomerId(),
            WebappCredentials.builder()
                             .customerName(customerData.getCustomerName())
                             .email(email)
                             .source(CustomerService.Source.HEROKU)
                             .build());
    }

    @Value
    @Builder
    public static class TokenFactory {
        private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        private final long jwtExpirationHours;
        private final String jwtSecret;

        private Date calculateExpirationDate() {
            Long hours = jwtExpirationHours;
            Duration duration = hours <= 0L ? Duration.ofMinutes(-hours) : Duration.ofHours(hours);
            logger.debug("The session token will live for {}", duration);
            return Date.from(Instant.now().plus(duration));
        }

        public String createWebappToken(Long customerId, WebappCredentials credentials) {
            return Jwts.builder()
                       .setId(Long.toString(customerId))
                       .setSubject(credentials.getCustomerName())
                       .setIssuedAt(new Date())
                       .setExpiration(calculateExpirationDate())
                       .claim(JWT_CLAIM_EMAIL, credentials.getEmail())
                       .claim(JWT_CLAIM_SOURCE, credentials.getSource())
                       .signWith(signatureAlgorithm, jwtSecret.getBytes(StandardCharsets.UTF_8))
                       .compact();
        }
    }

    static String maskSecondHalf(String s) {
        int pos = s.length() / 2;
        StringBuilder sb = new StringBuilder(s.substring(0, pos));
        while (sb.length() < s.length()) {
            sb.append('X');
        }
        return sb.toString();
    }
}
