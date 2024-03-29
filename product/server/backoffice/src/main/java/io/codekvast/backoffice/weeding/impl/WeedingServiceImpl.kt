/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
package io.codekvast.backoffice.weeding.impl

import io.codekvast.backoffice.metrics.BackofficeMetricsService
import io.codekvast.backoffice.weeding.WeedingService
import io.codekvast.common.aspects.Restartable
import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.logging.LoggingUtils.humanReadableDuration
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
                                             private val lockTemplate: LockTemplate,
                                             private val backofficeMetricsService: BackofficeMetricsService
) : WeedingService {

    val logger = LoggerFactory.getLogger(this.javaClass)!!

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
    override fun performDataWeeding() {
        val startedAt = clock.instant()
        logger.debug("Performing data weeding")

        val invocationsBefore = countRows("invocations")
        val methodsBefore = countRows("methods")
        val methodLocationsBefore = countRows("method_locations")

        val deletedJvms = jdbcTemplate.update("DELETE FROM jvms WHERE garbage = TRUE ORDER BY id")
        val deletedSyntheticMethods = jdbcTemplate.update("DELETE FROM methods WHERE garbage = TRUE ORDER BY id")

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
                WHERE e.enabled = TRUE AND j.environmentId IS NULL""")
            deletedRows += deletedEnvironments
        }

        val deletedAgents = jdbcTemplate.update("DELETE FROM agent_state WHERE garbage = TRUE ORDER BY id")
        deletedRows += deletedAgents

        val deletedRabbitMessageIds = jdbcTemplate.update("""
                DELETE FROM rabbitmq_message_ids WHERE receivedAt < ?
                ORDER BY messageId""".trimMargin(),
                Timestamp.from(clock.instant().minus(1, ChronoUnit.HOURS)))
        deletedRows += deletedRabbitMessageIds

        if (deletedRows > 0) {
            logger.info(String.format("Deleted %,d database rows (%,d agents, %,d JVMs, %,d methods (of which %,d were synthetic), %,d method locations, %,d applications, %,d environments, %,d invocations and %,d RabbitMQ messageIds) in %s.",
                    deletedRows, deletedAgents, deletedJvms, deletedMethods, deletedSyntheticMethods, deletedMethodLocations, deletedApplications, deletedEnvironments, deletedInvocations,
                    deletedRabbitMessageIds, humanReadableDuration(startedAt, clock.instant())))
        } else {
            logger.debug("Found nothing to delete")
        }
        backofficeMetricsService.countWeededRows(deletedRows)
    }

    private fun countRows(table: String) = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM $table", Int::class.java)!!

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
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
        val deadMarginSeconds = 600L
        if (cd.pricePlan.retentionPeriodDays > 0) {
            var startedAt = clock.instant()
            val now = clock.instant()

            val deadIfNotPolledForSeconds = cd.pricePlan.pollIntervalSeconds + deadMarginSeconds
            val deadIfNotPolledAfter = Timestamp.from(now.minus(deadIfNotPolledForSeconds, ChronoUnit.SECONDS))
            logger.debug("Finding dead agents for the customer {} which have not polled in the last {} minutes", cd.customerId, deadIfNotPolledForSeconds / 60)

            var count = jdbcTemplate.update("UPDATE agent_state SET garbage = TRUE WHERE customerId = ? AND lastPolledAt < ? AND garbage = FALSE",
                    cd.customerId, deadIfNotPolledAfter)
            if (count == 0) {
                logger.debug("Found no dead agents for the customer {}", cd.customerId)
            } else {
                logger.info("Marked {} agents as garbage for the customer {}", count, cd.customerId)
            }
            sum += count

            val deadIfNotPublishedForSeconds = cd.pricePlan.publishIntervalSeconds + deadMarginSeconds
            val deadIfNotPublishedAfter = Timestamp.from(now.minus(deadIfNotPublishedForSeconds, ChronoUnit.SECONDS))
            logger.debug("Finding dead JVMs for the customer {} which have not published anything in the last {} minutes", cd.customerId, deadIfNotPublishedForSeconds / 60)

            count = jdbcTemplate.update("UPDATE jvms SET garbage = TRUE WHERE customerId = ? AND publishedAt < ? AND garbage = FALSE",
                    cd.customerId, deadIfNotPublishedAfter)
            if (count == 0) {
                logger.debug("Found no dead JVMs for the customer {} in {}", cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            } else {
                logger.info("Marked {} JVMs as garbage for the customer {} in {}", count, cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            }

            startedAt = clock.instant()
            count = jdbcTemplate.update("UPDATE methods m, synthetic_signature_patterns p " +
                    "SET m.garbage = TRUE " +
                    "WHERE m.customerId = ? " +
                    "AND m.signature REGEXP p.pattern " +
                    "AND p.errorMessage IS NULL ",
                    cd.customerId)
            if (count == 0) {
                logger.debug("Found no synthetic methods for the customer {} in {}", cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            } else {
                logger.info("Marked {} methods as garbage for the customer {} in {}", count, cd.customerId, humanReadableDuration(startedAt, clock.instant()))
            }
            sum += count
        }
        return sum
    }
}
