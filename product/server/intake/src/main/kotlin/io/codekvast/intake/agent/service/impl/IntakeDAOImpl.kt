/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
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
package io.codekvast.intake.agent.service.impl

import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.intake.metrics.AgentStatistics
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/** @author olle.hallin@crisp.se
 */
@Repository
class IntakeDAOImpl(private val jdbcTemplate: JdbcTemplate) : IntakeDAO {

    val logger by LoggerDelegate()

    override fun writeLockAgentStateForCustomer(customerId: Long) {
        val startedAt = Instant.now()
        val ids = jdbcTemplate.queryForList(
                "SELECT id FROM agent_state WHERE customerId = ? AND garbage = FALSE ORDER BY customerId, jvmUuid "
                        + "LOCK IN SHARE MODE WAIT 30",
                Long::class.java,
                customerId
        )

        logger.info( // TODO: change to debug
                "Locked {} agent_state rows belonging to customer {} in {}",
                ids.size,
                customerId,
                Duration.between(startedAt, Instant.now())
        )
    }

    override fun markDeadAgentsAsGarbage(
            customerId: Long, thisJvmUuid: String, nextPollExpectedBefore: Instant
    ) {
        // Mark all agents that have not polled recently as garbage
        val updated = jdbcTemplate.update(
                "UPDATE agent_state SET garbage = TRUE "
                        + "WHERE customerId = ? AND jvmUuid != ? AND garbage = FALSE AND nextPollExpectedAt < ? ",
                customerId,
                thisJvmUuid,
                Timestamp.from(nextPollExpectedBefore)
        )
        if (updated > 0) {
            logger.info("Detected {} dead agents for customer {}", updated, customerId)
        }
    }

    override fun setAgentTimestamps(
            customerId: Long, thisJvmUuid: String, thisPollAt: Instant, nextExpectedPollAt: Instant
    ) {
        val thisPollAtTimestamp = Timestamp.from(thisPollAt)
        val nextExpectedPollTimestamp = Timestamp.from(nextExpectedPollAt)
        val updated = jdbcTemplate.update(
                "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, garbage = FALSE WHERE customerId = ? AND jvmUuid = ? ",
                thisPollAtTimestamp,
                nextExpectedPollTimestamp,
                customerId,
                thisJvmUuid
        )
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO agent_state(customerId, jvmUuid, lastPolledAt, nextPollExpectedAt, enabled, garbage) VALUES (?, ?, ?, ?, TRUE, FALSE)",
                    customerId,
                    thisJvmUuid,
                    thisPollAtTimestamp,
                    nextExpectedPollTimestamp
            )
            logger.info("The agent {}:'{}' has started", customerId, thisJvmUuid)
        } else {
            logger.debug("The agent {}:'{}' has polled", customerId, thisJvmUuid)
        }
    }

    override fun getNumOtherEnabledAliveAgents(
            customerId: Long, thisJvmUuid: String, nextPollExpectedAfter: Instant
    ): Int {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM agent_state "
                        + "WHERE enabled = TRUE AND garbage = FALSE AND customerId = ? AND nextPollExpectedAt >= ? AND jvmUuid != ? ",
                Int::class.java,
                customerId,
                Timestamp.from(nextPollExpectedAfter),
                thisJvmUuid
        )
    }

    @Cacheable(ENVIRONMENTS_CACHE)
    override fun isEnvironmentEnabled(customerId: Long, thisJvmUuid: String): Boolean {
        // At the first poll from a new environment, no data has yet been published. The environment is
        // part of the common publication data.
        val list = jdbcTemplate.queryForList(
                "SELECT enabled FROM environments e, jvms j "
                        + "WHERE e.customerId = ? AND e.id = j.environmentId AND j.uuid = ? ",
                Boolean::class.java,
                customerId,
                thisJvmUuid
        )
        // If this is the first poll, return true. Or else nothing will be published.
        return list.isEmpty() || list[0]
    }

    @Cacheable(ENVIRONMENTS_CACHE)
    override fun getEnvironmentName(jvmUuid: String): Optional<String> {
        val names = jdbcTemplate.queryForList(
                "SELECT name FROM environments e, jvms j "
                        + "WHERE e.id = j.environmentId AND j.uuid = ? ",
                String::class.java,
                jvmUuid
        )
        // If this is the first poll, return empty.
        return if (names.isEmpty()) Optional.empty() else Optional.of(
                names[0]
        )
    }

    override fun updateAgentEnabledState(customerId: Long, thisJvmUuid: String, enabled: Boolean) {
        jdbcTemplate.update(
                "UPDATE agent_state SET enabled = ? WHERE customerId = ? AND jvmUuid = ? ",
                enabled,
                customerId,
                thisJvmUuid
        )
    }

    override fun getAgentStatistics(nextPollExpectedAfter: Instant): AgentStatistics {
        val numDisabled = AtomicInteger()
        val numDead = AtomicInteger()
        val numAlive = AtomicInteger()
        jdbcTemplate.query(
                "SELECT enabled, nextPollExpectedAt FROM agent_state WHERE garbage = FALSE"
        ) { rs ->
            val enabled: Boolean = rs.getBoolean("enabled")
            val nextPollExpectedAt: Instant = rs.getTimestamp("nextPollExpectedAt").toInstant()
            val alive = enabled && nextPollExpectedAt.isAfter(nextPollExpectedAfter)
            numDisabled.addAndGet(if (!enabled) 1 else 0)
            numDead.addAndGet(if (enabled && !alive) 1 else 0)
            numAlive.addAndGet(if (alive) 1 else 0)
        }
        return AgentStatistics(
                numDisabled = numDisabled.get(),
                numDead = numDead.get(),
                numAlive = numAlive.get()
        )
    }

    companion object {
        private const val ENVIRONMENTS_CACHE = "environments"
    }
}
