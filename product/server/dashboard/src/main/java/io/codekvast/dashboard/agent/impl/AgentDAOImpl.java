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
package io.codekvast.dashboard.agent.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author olle.hallin@crisp.se
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AgentDAOImpl implements AgentDAO {

    private static final String ENVIRONMENTS_CACHE = "environments";
    private static final String CODEBASE_FINGERPRINTS_CACHE = "codeBaseFingerprints";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void disableDeadAgents(long customerId, String thisJvmUuid, Instant nextPollExpectedBefore) {
        // Disable all agents that have been dead for more than two file import intervals...
        int updated = jdbcTemplate.update("UPDATE agent_state SET enabled = FALSE " +
                                              "WHERE customerId = ? AND jvmUuid != ? AND enabled = TRUE AND nextPollExpectedAt < ? " +
                                              "ORDER BY id ",
                                          customerId, thisJvmUuid, Timestamp.from(nextPollExpectedBefore));
        if (updated > 0) {
            logger.info("Disabled {} dead agents for customer {}", updated, customerId);
        }
    }

    @Override
    public void setAgentTimestamps(long customerId, String thisJvmUuid, Instant thisPollAt, Instant nextExpectedPollAt) {
        Timestamp nextExpectedPollTimestamp = Timestamp.from(nextExpectedPollAt);

        int updated =
            jdbcTemplate.update(
                "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, garbage = ? WHERE customerId = ? AND jvmUuid = ? ",
                Timestamp.from(thisPollAt), nextExpectedPollTimestamp, FALSE, customerId, thisJvmUuid);
        if (updated == 0) {
            jdbcTemplate
                .update(
                    "INSERT INTO agent_state(customerId, jvmUuid, lastPolledAt, nextPollExpectedAt, enabled, garbage) VALUES (?, ?, ?, ?," +
                        " ?, ?)",
                    customerId, thisJvmUuid, Timestamp.from(thisPollAt), nextExpectedPollTimestamp, TRUE, FALSE);

            logger.info("The agent {}:'{}' has started", customerId, thisJvmUuid);
        } else {
            logger.debug("The agent {}:'{}' has polled", customerId, thisJvmUuid);
        }

    }

    @Override
    public int getNumOtherAliveAgents(long customerId, String thisJvmUuid, Instant nextPollExpectedAfter) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM agent_state " +
                                               "WHERE enabled = TRUE AND customerId = ? AND nextPollExpectedAt >= ? AND jvmUuid != ? ",
                                           Integer.class, customerId, Timestamp.from(nextPollExpectedAfter), thisJvmUuid);
    }

    @Override
    @Cacheable(ENVIRONMENTS_CACHE)
    public boolean isEnvironmentEnabled(long customerId, String thisJvmUuid) {
        // At the first poll from a new environment, no data has yet been published. The environment is part of the common publication data.
        List<Boolean> list = jdbcTemplate.queryForList("SELECT enabled FROM environments e, jvms j " +
                                                           "WHERE e.customerId = ? AND e.id = j.environmentId AND j.uuid = ? ",
                                                       Boolean.class, customerId, thisJvmUuid);
        // If this is the first poll, return true. Or else nothing will be published.
        return list.isEmpty() ? true : list.get(0);
    }

    @Override
    @Cacheable(ENVIRONMENTS_CACHE)
    public Optional<String> getEnvironmentName(String jvmUuid) {
        List<String> names = jdbcTemplate.queryForList("SELECT name FROM environments e, jvms j " +
                                                           "WHERE e.id = j.environmentId AND j.uuid = ? ",
                                                       String.class, jvmUuid);
        // If this is the first poll, return empty.
        return names.isEmpty() ? Optional.empty() : Optional.of(names.get(0));
    }

    @Override
    public void updateAgentEnabledState(long customerId, String thisJvmUuid, boolean enabled) {
        jdbcTemplate.update("UPDATE agent_state SET enabled = ? WHERE customerId = ? AND jvmUuid = ?", enabled, customerId, thisJvmUuid);
    }

}
