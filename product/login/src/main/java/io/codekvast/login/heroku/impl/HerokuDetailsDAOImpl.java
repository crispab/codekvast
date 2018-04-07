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
package io.codekvast.login.heroku.impl;

import io.codekvast.common.security.CipherException;
import io.codekvast.common.security.CipherUtils;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuDetailsDAO;
import io.codekvast.login.heroku.model.HerokuOAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * @author olle.hallin@crisp.se
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class HerokuDetailsDAOImpl implements HerokuDetailsDAO {

    private final JdbcTemplate jdbcTemplate;
    private final CodekvastLoginSettings settings;

    @Override
    public boolean existsRow(String licenseKey) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1)\n" +
                                               "FROM customers c INNER JOIN heroku_details hd ON c.id = hd.customerId\n" +
                                               "WHERE c.licensekey = ?", Integer.class, licenseKey) > 0;
    }

    @Override
    public void saveTokens(HerokuOAuthTokenResponse tokenResponse, String callbackUrl, String licenseKey) throws CipherException {
        Long customerId = jdbcTemplate.queryForObject("SELECT id FROM customers WHERE licenseKey = ?", Long.class, licenseKey);

        Instant expiresAt = Instant.now().plusSeconds(tokenResponse.getExpires_in() - 60);

        jdbcTemplate.update("INSERT INTO heroku_details (customerId, callbackUrl, accessToken, refreshToken, tokenType, expiresAt)\n" +
                                "VALUES (?, ?, ?, ?, ?, ?)",
                            customerId, callbackUrl,
                            CipherUtils.encrypt(tokenResponse.getAccess_token(), settings.getCipherSecret()),
                            CipherUtils.encrypt(tokenResponse.getRefresh_token(), settings.getCipherSecret()),
                            tokenResponse.getToken_type(),
                            Timestamp.from(expiresAt));

        logger.info("Saved OAuth tokens for customer {}", customerId);
        if (expiresAt.isBefore(Instant.now())) {
            logger.warn("The access token has already expired at {}", expiresAt);
        } else {
            logger.debug("The access token expires at {}", expiresAt);
        }
    }
}
