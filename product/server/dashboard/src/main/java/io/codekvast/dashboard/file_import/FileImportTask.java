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

import io.codekvast.common.thread.NamedThreadTemplate;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.metrics.PublicationMetricsService;
import java.io.File;
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

  @PostConstruct
  public void postConstruct() {
    logger.info(
        "Looking for files in {} every {} seconds",
        settings.getFileImportQueuePath(),
        settings.getFileImportIntervalSeconds());
  }

  @Scheduled(
      initialDelayString = "${codekvast.fileImportInitialDelaySeconds:5}000",
      fixedRateString = "${codekvast.fileImportIntervalSeconds}000")
  public void importPublicationFiles() {
    new NamedThreadTemplate().doInNamedThread("File Importer", this::run);
  }

  private void run() {
    File queuePath = settings.getFileImportQueuePath();
    int queueLength = countFiles(queuePath);
    metricsService.gaugePublicationQueueLength(queueLength);
    if (queueLength > 0) {
      logger.info("Importing {} new publication files", queueLength);
      processFiles(queuePath);
    }
  }

  private void processFiles(File path) {
    if (path != null) {
      File[] files = path.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            processFiles(file);
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

  private int countFiles(File path) {
    int result = 0;
    if (path != null) {
      File[] files = path.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            result += countFiles(file);
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
