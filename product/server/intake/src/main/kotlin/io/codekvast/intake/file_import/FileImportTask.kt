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
package io.codekvast.intake.file_import

import io.codekvast.common.lock.Lock
import io.codekvast.common.lock.LockTemplate
import io.codekvast.common.logging.LoggerDelegate
import io.codekvast.common.logging.LoggingUtils
import io.codekvast.common.thread.NamedThreadTemplate
import io.codekvast.intake.bootstrap.CodekvastIntakeSettings
import io.codekvast.intake.metrics.IntakeMetricsService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.stream.Collectors
import javax.annotation.PostConstruct

/**
 * Scans a certain directory for files produced by the Codekvast agents and imports them to the
 * database.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
@RequiredArgsConstructor
class FileImportTask(
    private val settings: CodekvastIntakeSettings,
    private val publicationImporter: PublicationImporter,
    private val metricsService: IntakeMetricsService,
    private val lockTemplate: LockTemplate
) {
    private val logger by LoggerDelegate()

    @PostConstruct
    fun postConstruct() {
        logger.info(
            "Looking for files in {} every {} seconds",
            settings.fileImportQueuePath,
            settings.fileImportIntervalSeconds
        )
    }

    @Scheduled(
        initialDelayString = "\${codekvast.intake.fileImportInitialDelaySeconds:5}000",
        fixedRateString = "\${codekvast.intake.fileImportIntervalSeconds}000"
    )
    fun importPublicationFiles() {
        NamedThreadTemplate().doInNamedThread("import", this::processQueue)
    }

    private fun processQueue() {
        lockTemplate.doWithLock(Lock.forTask("fileImport", 120), this::doProcessQueue)
    }

    private fun doProcessQueue() {
        val queue: List<File> = collectFilesInQueue();
        val queueLength = queue.size
        if (queueLength > 0) {
            logger.info("Importing {} new publication files", queueLength)
            val startedAt = Instant.now()

            queue.forEach(this::doProcessFile)

            logger.info(
                "Imported {} new publications files in {}",
                queueLength,
                LoggingUtils.humanReadableDuration(startedAt, Instant.now())
            )
        }
    }

    private fun collectFilesInQueue(): List<File> {
        val queuePath = settings.fileImportQueuePath
        if (queuePath.mkdirs()) {
            logger.info("Created {}", queuePath.absolutePath)
        }

        Files.list(queuePath.toPath()).use {
            val result = it
                .peek { p -> logger.debug("Found {}", p) }
                .map(Path::toFile)
                .filter { file -> file.name.endsWith(".ser") }
                .collect(Collectors.toList())
            metricsService.gaugePublicationQueueLength(result.size)
            return result
        }
    }

    private fun doProcessFile(file: File) {
        val handled = publicationImporter.importPublicationFile(file)
        if (handled && settings.deleteImportedFiles) {
            deleteFile(file)
        }
    }

    private fun deleteFile(file: File) {
        val deleted = file.delete()
        if (deleted) {
            logger.debug("Deleted {}", file)
        } else {
            logger.warn("Could not delete {}", file)
        }
    }
}
