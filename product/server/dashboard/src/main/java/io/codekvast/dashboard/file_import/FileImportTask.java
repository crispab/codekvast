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
package io.codekvast.dashboard.file_import;

import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.thread.NamedThreadTemplate;
import io.codekvast.common.util.LoggingUtils;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.PublicationMetricsService;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scans a certain directory for files produced by the Codekvast agent and imports them to the
 * database.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FileImportTask {

  private final CodekvastDashboardSettings settings;
  private final PublicationImporter publicationImporter;
  private final PublicationMetricsService metricsService;
  private final LockTemplate lockTemplate;

  @PostConstruct
  public void postConstruct() {
    logger.info(
        "Looking for files in {} every {} seconds",
        settings.getFileImportQueuePath(),
        settings.getFileImportIntervalSeconds());
  }

  @Scheduled(
      initialDelayString = "${codekvast.dashboard.fileImportInitialDelaySeconds:5}000",
      fixedRateString = "${codekvast.dashboard.fileImportIntervalSeconds}000")
  public void importPublicationFiles() {
    lockTemplate.doWithLock(
        Lock.forTask("fileImport"),
        () -> {
          new NamedThreadTemplate().doInNamedThread("File Importer", this::processQueue);
        });
  }

  private void processQueue() {
    Instant startedAt = Instant.now();
    File queuePath = settings.getFileImportQueuePath();

    int queueLength = getQueueLength(queuePath);

    if (queueLength > 0) {
      logger.info("Importing {} new publication files", queueLength);

      doProcessQueue(queuePath);

      logger.info(
          "Imported {} new publications files in {}",
          queueLength,
          LoggingUtils.humanReadableDuration(startedAt, Instant.now()));
    }
  }

  private int getQueueLength(File queuePath) {
    Instant startedAt = Instant.now();

    int queueLength = doCountFiles(queuePath);

    Duration duration = Duration.between(startedAt, Instant.now());
    if (duration.toSeconds() >= 1) {
      logger.info(
          "Counted {} files in {} in {}",
          queueLength,
          queuePath,
          LoggingUtils.humanReadableDuration(duration));
    }
    metricsService.gaugePublicationQueueLength(queueLength);
    return queueLength;
  }

  private void doProcessQueue(File path) {
    if (path != null) {
      File[] files = path.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            doProcessQueue(file);
          } else if (file.getName().endsWith(".ser")) {
            boolean handled = publicationImporter.importPublicationFile(file);
            if (handled && settings.isDeleteImportedFiles()) {
              deleteFile(file);
            }
          } else {
            logger.debug("Ignoring {}", file);
          }
        }
      }
    }
  }

  private int doCountFiles(File path) {
    int result = 0;
    if (path != null) {
      File[] files = path.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            result += doCountFiles(file);
          } else if (file.isFile()) {
            result += 1;
          }
        }
      }
    }
    return result;
  }

  private void deleteFile(File file) {
    boolean deleted = file.delete();
    if (deleted) {
      logger.debug("Deleted {}", file);
    } else {
      logger.warn("Could not delete {}", file);
    }
  }
}
