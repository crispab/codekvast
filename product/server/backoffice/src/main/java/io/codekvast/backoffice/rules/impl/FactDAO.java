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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.backoffice.rules.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.codekvast.backoffice.facts.PersistentFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.Instant;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class FactDAO {

    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter()).create();

    @Transactional(propagation = Propagation.MANDATORY)
    public List<FactWrapper> getFacts(Long customerId) {
        List<FactWrapper> wrappers = jdbcTemplate.query("SELECT id, type, data FROM facts WHERE customerId = ?", (rs, rowNum) -> {
            Long id = rs.getLong("id");
            String type = rs.getString("type");
            String data = rs.getString("data");
            try {
                Object fact = gson.fromJson(data, Class.forName(type));
                return new FactWrapper(id, (PersistentFact) fact);
            } catch (ClassCastException | ClassNotFoundException e) {
                throw new SQLDataException("Cannot load fact of type '" + type + "'", e);
            }
        }, customerId);

        logger.trace("Retrieved the persistent facts {} for customer {}", wrappers, customerId);
        logger.debug("Retrieved {} persistent facts for customer {}", wrappers.size(), customerId);
        return wrappers;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long addFact(Long customerId, PersistentFact fact) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(new AddFactStatementCreator(customerId, getType(fact), gson.toJson(fact)), keyHolder);
        long factId = keyHolder.getKey().longValue();
        if (inserted <= 0) {
            logger.error("Attempt to insert duplicate fact {}:{}", customerId, factId);
        }
        return factId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateFact(Long customerId, Long factId, PersistentFact fact) {
        int updated = jdbcTemplate.update("UPDATE facts SET type = ?, data = ? WHERE id = ? AND customerId = ?",
                                          getType(fact), gson.toJson(fact), factId, customerId);
        if (updated <= 0) {
            logger.error("Failed to update fact {}:{}", customerId, factId);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void removeFact(Long customerId, Long factId) {
        int deleted = jdbcTemplate
            .update("DELETE FROM facts WHERE id = ? AND customerId = ?", factId, customerId);
        if (deleted <= 0) {
            logger.error("Failed to delete fact {}:{}", customerId, factId);
        }
    }

    private String getType(PersistentFact fact) {
        return fact.getClass().getName();
    }

    @RequiredArgsConstructor
    private static class AddFactStatementCreator implements PreparedStatementCreator {
        private final Long customerId;
        private final String type;
        private final String data;

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO facts(customerId, type, data) VALUES(?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId);
            ps.setString(2, type);
            ps.setString(3, data);
            return ps;
        }
    }

}
