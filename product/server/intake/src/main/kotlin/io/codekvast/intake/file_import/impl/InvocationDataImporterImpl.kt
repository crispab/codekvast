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
package io.codekvast.intake.file_import.impl

import io.codekvast.common.aspects.Restartable
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.logging.LoggingUtils.humanReadableDuration
import io.codekvast.common.messaging.EventService
import io.codekvast.common.messaging.model.InvocationDataReceivedEvent
import io.codekvast.intake.file_import.InvocationDataImporter
import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.model.PublicationType.INVOCATIONS
import io.codekvast.javaagent.model.v2.CommonPublicationData2
import io.codekvast.javaagent.model.v2.InvocationDataPublication2
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

/** @author olle.hallin@crisp.se
 */
@Component
class InvocationDataImporterImpl(
        private val commonImporter: CommonImporter,
        private val importDAO: ImportDAO,
        private val syntheticSignatureService: SyntheticSignatureService,
        private val metricsService: IntakeMetricsService,
        private val eventService: EventService,
        private val lockTemplate: LockTemplate,
        private val clock: Clock
) : InvocationDataImporter {

    private val logger by LoggerDelegate()

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
    override fun importPublication(publication: InvocationDataPublication2): Boolean {
        logger.debug("Importing {}", publication)
        val data = publication.commonData
        val invocations = publication.invocations.stream()
                .filter { !syntheticSignatureService.isSyntheticMethod(it) }
                .collect(Collectors.toSet())
        val ignoredSyntheticSignatures = publication.invocations.size - invocations.size
        val duration = lockTemplate.doWithLockOrThrow(Lock.forCustomer(data.customerId)) {
            doImportInvocations(
                    publication.recordingIntervalStartedAtMillis, data, invocations
            )
        }
        logger.info(
                "Imported {} in {} (ignoring {} synthetic signatures)",
                publication,
                humanReadableDuration(duration),
                ignoredSyntheticSignatures
        )
        metricsService.recordImportedPublication(
                INVOCATIONS, invocations.size, ignoredSyntheticSignatures, duration
        )
        return true
    }

    private fun doImportInvocations(
            recordingIntervalStartedAtMillis: Long,
            data: CommonPublicationData2,
            invocations: Set<String>
    ): Duration {
        val startedAt = clock.instant()
        val importContext: CommonImporter.ImportContext = commonImporter.importCommonData(data)
        importDAO.importInvocations(importContext, recordingIntervalStartedAtMillis, invocations)
        eventService.send(
                InvocationDataReceivedEvent.builder()
                        .customerId(data.customerId)
                        .appName(data.appName)
                        .appVersion(data.appVersion)
                        .agentVersion(data.agentVersion)
                        .environment(data.environment)
                        .hostname(data.hostname)
                        .size(invocations.size)
                        .receivedAt(Instant.ofEpochMilli(data.publishedAtMillis))
                        .trialPeriodEndsAt(importContext.trialPeriodEndsAt)
                        .build()
        )
        return Duration.between(startedAt, clock.instant())
    }
}
