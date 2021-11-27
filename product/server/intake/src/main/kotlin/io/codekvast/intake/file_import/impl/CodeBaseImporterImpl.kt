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
package io.codekvast.intake.file_import.impl

import io.codekvast.common.aspects.Restartable
import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.logging.LoggingUtils.humanReadableDuration
import io.codekvast.common.messaging.EventService
import io.codekvast.common.messaging.model.CodeBaseReceivedEvent
import io.codekvast.intake.file_import.CodeBaseImporter
import io.codekvast.intake.metrics.IntakeMetricsService
import io.codekvast.intake.model.PublicationType.CODEBASE
import io.codekvast.javaagent.model.v2.CommonPublicationData2
import io.codekvast.javaagent.model.v3.CodeBaseEntry3
import io.codekvast.javaagent.model.v3.CodeBasePublication3
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

/** @author olle.hallin@crisp.se
 */
@Component
internal class CodeBaseImporterImpl(
        private val commonImporter: CommonImporter,
        private val importDAO: ImportDAO,
        private val syntheticSignatureService: SyntheticSignatureService,
        private val metricsService: IntakeMetricsService,
        private val eventService: EventService,
        private val lockTemplate: LockTemplate,
        private val clock: Clock
) : CodeBaseImporter {

    private val logger by LoggerDelegate()

    @Transactional(rollbackFor = [Exception::class])
    @Restartable
    override fun importPublication(publication: CodeBasePublication3): Boolean {
        logger.debug("Importing {}", publication)
        val data = publication.commonData
        val entries: Collection<CodeBaseEntry3> = publication.entries.stream()
                .filter { e: CodeBaseEntry3 -> !syntheticSignatureService.isSyntheticMethod(e.signature) }
                .collect(Collectors.toList())
        val ignoredSyntheticSignatures = publication.entries.size - entries.size
        val duration = lockTemplate.doWithLockOrThrow(
                Lock.forCustomer(data.customerId)
        ) { doImportCodeBase(data, entries) }
        logger.info(
                "Imported {} in {} (ignoring {} synthetic signatures)",
                publication,
                humanReadableDuration(duration),
                ignoredSyntheticSignatures
        )
        metricsService.recordImportedPublication(
                CODEBASE, entries.size, ignoredSyntheticSignatures, duration
        )
        return true
    }

    private fun doImportCodeBase(
            data: CommonPublicationData2, entries: Collection<CodeBaseEntry3>
    ): Duration {
        val startedAt = clock.instant()
        val importContext = commonImporter.importCommonData(data)
        val isNewCodebase = importDAO.importCodeBaseFingerprint(data, importContext)
        if (isNewCodebase) {
            importDAO.importMethods(data, importContext, entries)
        }
        eventService.send(
                CodeBaseReceivedEvent.builder()
                        .customerId(data.customerId)
                        .appName(data.appName)
                        .appVersion(data.appVersion)
                        .agentVersion(data.agentVersion)
                        .environment(data.environment)
                        .hostname(data.hostname)
                        .size(entries.size)
                        .receivedAt(Instant.ofEpochMilli(data.publishedAtMillis))
                        .trialPeriodEndsAt(importContext.trialPeriodEndsAt)
                        .build()
        )
        return Duration.between(startedAt, clock.instant())
    }
}