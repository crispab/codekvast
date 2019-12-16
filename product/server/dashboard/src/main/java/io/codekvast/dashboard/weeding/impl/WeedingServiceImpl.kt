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
package io.codekvast.dashboard.weeding.impl

import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.util.LoggingUtils.humanReadableDuration
import io.codekvast.dashboard.weeding.WeedingService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Service which keeps the database tidy by removing garbage.
 *
 * @author olle.hallin@crisp.se
 */
@Service
class WeedingServiceImpl @Inject constructor(private val jdbcTemplate: JdbcTemplate,
                                             private val customerService: CustomerService,
                                             private val clock: Clock,
                                             private val lockTemplate: LockTemplate) : WeedingService {

    val logger = LoggerFactory.getLogger(this.javaClass)!!

    override fun performDataWeeding() {
        val startedAt = clock.instant()
        logger.debug("Performing data weeding")

        val invocationsBefore = countRows("invocations")
        val methodsBefore = countRows("methods")
        val methodLocationsBefore = countRows("method_locations")

        val deletedJvms = jdbcTemplate.update("DELETE FROM jvms WHERE garbage = TRUE ")
        val deletedSyntheticMethods = jdbcTemplate.update("DELETE FROM methods WHERE garbage = TRUE ")

        var deletedMethodLocations = 0
        var deletedMethods = 0
        var deletedApplications = 0
        var deletedEnvironments = 0
        var deletedInvocations = 0

        var deletedRows = deletedJvms + deletedSyntheticMethods
        if (deletedRows > 0) {
            // Delete methods without invocations
            jdbcTemplate.update("""
                DELETE m FROM methods AS m
                LEFT JOIN invocations AS i ON m.id = i.methodId
                WHERE i.methodId IS NULL""")

            deletedInvocations = invocationsBefore - countRows("invocations")
            deletedMethods = methodsBefore - countRows("methods")
            deletedMethodLocations = methodLocationsBefore - countRows("method_locations")
            deletedRows += deletedInvocations + deletedMethods + deletedMethodLocations

            deletedApplications = jdbcTemplate.update("""
                DELETE a FROM applications AS a
                LEFT JOIN jvms AS j ON a.id = j.applicationId
                WHERE j.applicationId IS NULL""")
            deletedRows += deletedApplications

            deletedEnvironments = jdbcTemplate.update("""
                DELETE e FROM environments AS e
                LEFT JOIN jvms AS j ON e.id = j.environmentId
                WHERE j.environmentId IS NULL""")
            deletedRows += deletedEnvironments
        }

        val deletedAgents = jdbcTemplate.update("DELETE FROM agent_state WHERE garbage = TRUE ")
        deletedRows += deletedAgents

        val deletedRabbitMessageIds = jdbcTemplate.update("DELETE FROM rabbitmq_message_ids WHERE receivedAt < ?",
            Timestamp.from(clock.instant().minus(1, ChronoUnit.HOURS)))
        deletedRows += deletedRabbitMessageIds

        if (deletedRows > 0) {
            logger.info(String.format("Deleted %,d database rows (%,d agents, %,d JVMs, %,d methods (of which %,d were synthetic), %,d method locations, %,d applications, %,d environments, %,d invocations and %,d RabbitMQ messageIds) in %s.",
                deletedRows, deletedAgents, deletedJvms, deletedMethods, deletedSyntheticMethods, deletedMethodLocations, deletedApplications, deletedEnvironments, deletedInvocations,
                deletedRabbitMessageIds, humanReadableDuration(startedAt, clock.instant())))
        } else {
            logger.debug("Found nothing to delete")
        }
    }

    private fun countRows(table: String) = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM $table", Int::class.java)!!

    override fun findWeedingCandidates() {
        val startedAt = clock.instant()
        var sum = 0
        for (cd in customerService.customerData.sortedBy { customerData: CustomerData -> customerData.customerId }) {
            if (cd.customerId > 0) {
                sum += lockTemplate.doWithLock(Lock.forCustomer(cd.customerId), { findWeedingCandidatesForCustomer(cd) }, { 0 })
            }
        }
        logger.info("{} weeding candidates identified in {}", sum, humanReadableDuration(startedAt, clock.instant()))
    }

    private fun findWeedingCandidatesForCustomer(cd: CustomerData): Int {
        var sum = 0
        val retentionPeriodDays = cd.pricePlan.retentionPeriodDays
        if (retentionPeriodDays > 0) {
            var startedAt = clock.instant();
            val now = clock.instant()
            val retentionPeriodStart = now.minus(retentionPeriodDays.toLong(), ChronoUnit.DAYS)
            val deadIfNotPolledAfter = now.minus(5, ChronoUnit.MINUTES)
            logger.debug("Finding dead agents and JVMs for customer {} which are older than {} days", cd.customerId, retentionPeriodDays)

            var count = jdbcTemplate.update("UPDATE agent_state SET garbage = TRUE " +
                "WHERE customerId = ? AND createdAt < ? AND lastPolledAt < ? ",
                cd.customerId, Timestamp.from(retentionPeriodStart), Timestamp.from(deadIfNotPolledAfter))
            if (count == 0) {
                logger.debug("Found no dead agents for customer {}", cd.customerId)
            } else {
                logger.info("Marked {} agents as garbage for customer {}", count, cd.customerId)
            }
            sum += count

            count = jdbcTemplate.update("UPDATE jvms SET garbage = TRUE WHERE customerId = ? AND publishedAt < ? ",
                cd.customerId, Timestamp.from(retentionPeriodStart))
            if (count == 0) {
                logger.debug("Found no dead JVMs for customer {} in {}", cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            } else {
                logger.info("Marked {} JVMs as garbage for customer {} in {}", count, cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            }

            startedAt = clock.instant();
            count = jdbcTemplate.update("UPDATE methods m, synthetic_signature_patterns p " +
                "SET m.garbage = TRUE " +
                "WHERE m.customerId = ? " +
                "AND m.signature REGEXP p.pattern " +
                "AND p.errorMessage IS NULL ",
                cd.customerId)
            if (count == 0) {
                logger.debug("Found no synthetic methods for customer {} in {}", cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            } else {
                logger.info("Marked {} methods as garbage for customer {} in {}", count, cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            }
            sum += count
        }
        return sum
    }
}
