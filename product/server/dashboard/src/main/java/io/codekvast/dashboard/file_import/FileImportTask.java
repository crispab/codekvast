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
import io.codekvast.dashboard.metrics.AgentMetricsService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
  private final AgentMetricsService metricsService;
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
    new NamedThreadTemplate().doInNamedThread("FileImportTask", this::processQueue);
  }

  @SneakyThrows
  private void processQueue() {
    List<File> queue =
        lockTemplate.doWithLock(
            Lock.forTask("fileImport", 10),
            () -> collectFiles(settings.getFileImportQueuePath()),
            () -> Collections.emptyList());

    int queueLength = queue.size();

    if (queueLength > 0) {
      logger.info("Importing {} new publication files", queueLength);

      Instant startedAt = Instant.now();

      queue.forEach(this::doProcessFile);

      logger.info(
          "Imported {} new publications files in {}",
          queueLength,
          LoggingUtils.humanReadableDuration(startedAt, Instant.now()));
    }
  }

  @SneakyThrows(IOException.class)
  private List<File> collectFiles(File queuePath) {
    try (Stream<Path> pathStream = Files.list(queuePath.toPath())) {
      List<File> result =
          pathStream
              .peek(p -> logger.debug("Found {}", p))
              .map(Path::toFile)
              .filter(file -> file.getName().endsWith(".ser"))
              .collect(Collectors.toList());
      metricsService.gaugePublicationQueueLength(result.size());
      return result;
    }
  }

  public void doProcessFile(File file) {
    boolean handled = publicationImporter.importPublicationFile(file);
    if (handled && settings.isDeleteImportedFiles()) {
      deleteFile(file);
    }
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
