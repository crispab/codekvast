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
package io.codekvast.dashboard.weeding.impl

import io.codekvast.common.customer.CustomerService
import io.codekvast.dashboard.util.LoggingUtils
import io.codekvast.dashboard.weeding.WeedingService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.text.*

/**
 * Service that keeps the database tidy by removing child-less rows in methods, applications and environments.
 *
 * @author olle.hallin@crisp.se
 */
@Service
class WeedingServiceImpl @Inject constructor(private val jdbcTemplate: JdbcTemplate,
                                             private val customerService: CustomerService,
                                             private val clock: Clock) : WeedingService {

    val logger = LoggerFactory.getLogger(this.javaClass)!!

    @Transactional(rollbackFor = [Exception::class])
    override fun performDataWeeding() {
        val startedAt = Instant.now()
        logger.debug("Performing data weeding")

        val invocationsBefore = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM invocations", Int::class.java)!!
        val deletedJvms = jdbcTemplate.update("DELETE FROM jvms WHERE garbage = TRUE ")
        var deletedMethods = 0
        var deletedApplications = 0
        var deletedEnvironments = 0
        var deletedInvocations = 0

        if (deletedJvms > 0) {
            val invocationsAfter = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM invocations", Int::class.java)!!
            deletedInvocations = invocationsBefore - invocationsAfter

            deletedMethods = jdbcTemplate.update("""
                DELETE m FROM methods AS m
                LEFT JOIN invocations AS i ON m.id = i.methodId
                WHERE i.methodId IS NULL""")

            deletedApplications = jdbcTemplate.update("""
                DELETE a FROM applications AS a
                LEFT JOIN jvms AS j ON a.id = j.applicationId
                WHERE j.applicationId IS NULL""")

            deletedEnvironments = jdbcTemplate.update("""
                DELETE e FROM environments AS e
                LEFT JOIN jvms AS j ON e.id = j.environmentId
                WHERE j.environmentId IS NULL""")
        }

        val deletedAgents = jdbcTemplate.update("DELETE FROM agent_state WHERE garbage = TRUE ")

        val deletedRows = deletedAgents + deletedJvms + deletedMethods + deletedApplications + deletedEnvironments + deletedInvocations
        if (deletedRows > 0) {
            logger.info(String.format("Deleted %,d database rows (%,d agents, %,d JVMs, %,d methods, %,d applications, %,d environments and %,d invocations) in %s.",
                deletedRows, deletedAgents, deletedJvms, deletedMethods, deletedApplications, deletedEnvironments, deletedInvocations,
                LoggingUtils.humanReadableDuration(Duration.between(startedAt, Instant.now()))))
        } else {
            logger.debug("Found nothing to delete")
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun findWeedingCandidates() {
        val startedAt = Instant.now()
        var sum = 0
        for (cd in customerService.customerData) {
            val retentionPeriodDays = cd.pricePlan.retentionPeriodDays
            if (retentionPeriodDays > 0) {
                val now = clock.instant()
                val retentionPeriodStart = now.minus(retentionPeriodDays.toLong(), ChronoUnit.DAYS)
                val deadIfNotPolledAfter = now.minus(5, ChronoUnit.MINUTES)
                logger.debug("Finding dead agents and JVMs for customer {} which are older than {} days", cd.customerId, retentionPeriodDays)

                var count = jdbcTemplate.update("UPDATE agent_state SET garbage = TRUE WHERE customerId = ? AND createdAt < ? AND lastPolledAt < ? AND garbage = FALSE ",
                    cd.customerId, Timestamp.from(retentionPeriodStart), Timestamp.from(deadIfNotPolledAfter))
                if (count == 0) {
                    logger.debug("Found no garbage agents for customer {}", cd.customerId)
                } else {
                    logger.debug("Marked {} agents as garbage for customer {}", count, cd.customerId)
                }
                sum += count

                count = jdbcTemplate.update("UPDATE jvms SET garbage = TRUE WHERE customerId = ? AND publishedAt < ? AND garbage = FALSE ",
                    cd.customerId, Timestamp.from(retentionPeriodStart))
                if (count == 0) {
                    logger.debug("Found no garbage JVMs for customer {}", cd.customerId)
                } else
                    logger.debug("Marked {} JVMs as garbage for customer {}", count, cd.customerId)
                sum += count
            }
        }
        logger.debug("{} weeding candidates identified in {}", sum, LoggingUtils.humanReadableDuration(Duration.between(startedAt, Instant.now())))
    }
}
