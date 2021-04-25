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
package io.codekvast.common.dump_management;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.logging.LoggingUtils;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.SlackService.Channel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A scheduled task that finds heap dumps (.hprof files) in a certain path and uploads them to S3.
 *
 * <p>See also google-jib.gradle
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HeapDumpUploader {
  private final CodekvastCommonSettings settings;
  private final SlackService slackService;

  private static final String SUFFIX = ".hprof";
  private final Map<File, FileStatus> fileStatuses = new HashMap<>();

  private boolean firstTime = true;

  @Scheduled(
      initialDelayString = "${codekvast.common.heapDumpUploaderDelaySeconds}000",
      fixedDelayString = "${codekvast.common.heapDumpUploaderIntervalSeconds}000")
  public void scanForHeapDumps() {
    String path = settings.getHeapDumpsPath();
    if (firstTime) {
      logger.info("Scanning for {} files in {} ...", SUFFIX, path);
    } else {
      logger.trace("Scanning for {} files in {} ...", SUFFIX, path);
    }
    firstTime = false;

    try (Stream<Path> stream = Files.walk(Path.of(path))) {
      stream.forEach(this::handlePath);
    } catch (IOException e) {
      logger.warn("Failed to scan {}: {}", path, e.toString());
    }
  }

  @PostConstruct
  public void validateS3Credentials() {
    val dumpPath = new File(settings.getHeapDumpsPath());
    if (!dumpPath.isDirectory()) {
      val created = dumpPath.mkdirs();
      if (created) {
        logger.info("Created {}", dumpPath);
      }
    }

    if (!"dev".equals(settings.getEnvironment())) {
      try {
        logger.debug("Validating S3 credentials...");
        val s3 = AmazonS3ClientBuilder.defaultClient();
        logger.debug("Validated S3 credentials for region '{}'", s3.getRegionName());
      } catch (Exception e) {
        logger.warn("Failed to validate S3 credentials, will not be able to upload heap dumps", e);
      }
    }
  }

  private void handlePath(Path path) {
    var file = path.toFile();
    if (file.isFile() && file.getName().endsWith(SUFFIX)) {
      FileStatus oldStatus = fileStatuses.get(file);
      FileStatus newStatus = FileStatus.of(file);
      if (oldStatus == null) {
        logger.debug("Found {}, waiting for silence", file);
        fileStatuses.put(file, newStatus);
      } else if (!newStatus.equals(oldStatus)) {
        logger.debug("{} is still being written: {} -> {}", file, oldStatus, newStatus);
        fileStatuses.put(file, newStatus);
      } else {
        logger.debug("{} is not growing anymore, uploading it to S3...", file);
        uploadFileToS3(file);
      }
    }
  }

  private void uploadFileToS3(File file) {
    val bucket = settings.getHeapDumpsBucket();
    val timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    val key =
        settings.getEnvironment()
            + "/"
            + file.getName().replace(SUFFIX, String.format("-%s%s", timestamp, SUFFIX));
    val startedAt = Instant.now();
    try {
      val renamed =
          Files.move(file.toPath(), Path.of(file.getPath() + ".uploading"), ATOMIC_MOVE).toFile();
      val s3 = AmazonS3ClientBuilder.defaultClient();
      s3.putObject(bucket, key, renamed);

      val message =
          String.format(
              "Uploaded %s (%s) to s3://%s/%s in %s",
              file,
              LoggingUtils.humanReadableByteCount(file.length()),
              bucket,
              key,
              Duration.between(startedAt, Instant.now()).toString());

      logger.info(message);
      slackService.sendNotification(message, Channel.ALARMS);

      if (file.delete()) {
        logger.info("Deleted {}", file);
      } else {
        logger.debug("Failed to delete {}. Perhaps deleted by other instance?", file);
      }
    } catch (NoSuchFileException e) {
      logger.debug("Some other instance has already processed {}", file);
    } catch (Exception e) {
      logger.error(String.format("Failed to put %s to s3://%s/%s", file, bucket, key), e);
    }
  }
}
