/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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

import io.codekvast.dashboard.metrics.AgentStatistics;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** @author olle.hallin@crisp.se */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AgentDAOImpl implements AgentDAO {

  private static final String ENVIRONMENTS_CACHE = "environments";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void writeLockAgentStateForCustomer(long customerId) {
    Instant startedAt = Instant.now();
    List<Long> ids =
        jdbcTemplate.queryForList(
            "SELECT id FROM agent_state WHERE customerId = ? AND garbage = FALSE ORDER BY customerId, jvmUuid "
                + "LOCK IN SHARE MODE WAIT 30",
            Long.class,
            customerId);
    logger.info( // TODO: change to debug
        "Locked {} agent_state rows belonging to customer {} in {}",
        ids.size(),
        customerId,
        Duration.between(startedAt, Instant.now()));
  }

  @Override
  public void markDeadAgentsAsGarbage(
      long customerId, String thisJvmUuid, Instant nextPollExpectedBefore) {
    // Mark all agents that have not polled recently as garbage
    int updated =
        jdbcTemplate.update(
            "UPDATE agent_state SET garbage = TRUE "
                + "WHERE customerId = ? AND jvmUuid != ? AND garbage = FALSE AND nextPollExpectedAt < ? ",
            customerId,
            thisJvmUuid,
            Timestamp.from(nextPollExpectedBefore));
    if (updated > 0) {
      logger.info("Detected {} dead agents for customer {}", updated, customerId);
    }
  }

  @Override
  public void setAgentTimestamps(
      long customerId, String thisJvmUuid, Instant thisPollAt, Instant nextExpectedPollAt) {

    Timestamp thisPollAtTimestamp = Timestamp.from(thisPollAt);
    Timestamp nextExpectedPollTimestamp = Timestamp.from(nextExpectedPollAt);

    int updated =
        jdbcTemplate.update(
            "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, garbage = FALSE WHERE customerId = ? AND jvmUuid = ? ",
            thisPollAtTimestamp,
            nextExpectedPollTimestamp,
            customerId,
            thisJvmUuid);
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO agent_state(customerId, jvmUuid, lastPolledAt, nextPollExpectedAt, enabled, garbage) VALUES (?, ?, ?, ?, TRUE, FALSE)",
          customerId,
          thisJvmUuid,
          thisPollAtTimestamp,
          nextExpectedPollTimestamp);

      logger.info("The agent {}:'{}' has started", customerId, thisJvmUuid);
    } else {
      logger.debug("The agent {}:'{}' has polled", customerId, thisJvmUuid);
    }
  }

  @Override
  public int getNumOtherEnabledAliveAgents(
      long customerId, String thisJvmUuid, Instant nextPollExpectedAfter) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(1) FROM agent_state "
            + "WHERE enabled = TRUE AND garbage = FALSE AND customerId = ? AND nextPollExpectedAt >= ? AND jvmUuid != ? ",
        Integer.class,
        customerId,
        Timestamp.from(nextPollExpectedAfter),
        thisJvmUuid);
  }

  @Override
  @Cacheable(ENVIRONMENTS_CACHE)
  public boolean isEnvironmentEnabled(long customerId, String thisJvmUuid) {
    // At the first poll from a new environment, no data has yet been published. The environment is
    // part of the common publication data.
    List<Boolean> list =
        jdbcTemplate.queryForList(
            "SELECT enabled FROM environments e, jvms j "
                + "WHERE e.customerId = ? AND e.id = j.environmentId AND j.uuid = ? ",
            Boolean.class,
            customerId,
            thisJvmUuid);
    // If this is the first poll, return true. Or else nothing will be published.
    return list.isEmpty() || list.get(0);
  }

  @Override
  @Cacheable(ENVIRONMENTS_CACHE)
  public Optional<String> getEnvironmentName(String jvmUuid) {
    List<String> names =
        jdbcTemplate.queryForList(
            "SELECT name FROM environments e, jvms j "
                + "WHERE e.id = j.environmentId AND j.uuid = ? ",
            String.class,
            jvmUuid);
    // If this is the first poll, return empty.
    return names.isEmpty() ? Optional.empty() : Optional.of(names.get(0));
  }

  @Override
  public void updateAgentEnabledState(long customerId, String thisJvmUuid, boolean enabled) {
    jdbcTemplate.update(
        "UPDATE agent_state SET enabled = ? WHERE customerId = ? AND jvmUuid = ? ",
        enabled,
        customerId,
        thisJvmUuid);
  }

  @Override
  public AgentStatistics getAgentStatistics(Instant nextPollExpectedAfter) {
    AtomicInteger numDisabled = new AtomicInteger();
    AtomicInteger numDead = new AtomicInteger();
    AtomicInteger numAlive = new AtomicInteger();

    jdbcTemplate.query(
        "SELECT enabled, nextPollExpectedAt FROM agent_state WHERE garbage = FALSE",
        rs -> {
          boolean enabled = rs.getBoolean("enabled");
          Instant nextPollExpectedAt = rs.getTimestamp("nextPollExpectedAt").toInstant();
          boolean alive = enabled && nextPollExpectedAt.isAfter(nextPollExpectedAfter);

          numDisabled.addAndGet(!enabled ? 1 : 0);
          numDead.addAndGet(enabled && !alive ? 1 : 0);
          numAlive.addAndGet(alive ? 1 : 0);
        });
    return AgentStatistics.builder()
        .numDisabled(numDisabled.get())
        .numDead(numDead.get())
        .numAlive(numAlive.get())
        .build();
  }
}
