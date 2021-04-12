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
package io.codekvast.s3_uploader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class CodekvastS3UploaderApplication extends TimerTask {
  private static final String S3_BUCKET = System.getenv("S3_BUCKET");
  private static final String SCAN_PATH = System.getenv("SCAN_PATH");
  private static final Integer SCAN_INTERVAL_SECONDS =
      Integer.parseInt(System.getenv("SCAN_INTERVAL_SECONDS"));

  private final Set<File> alreadyHandledFiles = new HashSet<>();
  private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

  @Override
  public void run() {
    logger.trace("Scanning {} for .hprof files...", SCAN_PATH);

    try (Stream<Path> stream = Files.walk(Path.of(SCAN_PATH))) {
      stream.forEach(this::handlePath);
    } catch (IOException e) {
      logger.debug("Failed to scan {}: {}", SCAN_PATH, e.toString());
    }
  }

  private void handlePath(Path path) {
    var file = path.toFile();
    if (file.isFile() && file.getName().endsWith(".hprof") && alreadyHandledFiles.add(file)) {
      uploadFileToS3(file);
    }
  }

  private void uploadFileToS3(File file) {
    val key = file.getName();
    try {
      val startedAt = Instant.now();

      s3.putObject(S3_BUCKET, key, file);

      logger.info(
          "Uploaded {} ({} bytes) to s3://{}/{} in {}",
          file,
          file.length(),
          S3_BUCKET,
          key,
          Duration.between(startedAt, Instant.now()));
      if (file.delete()) {
        logger.info("Deleted {}", file);
      } else {
        logger.warn("Failed to delete {}", file);
      }
    } catch (Exception e) {
      logger.error(String.format("Failed to put %s to s3://%s/%s", file, S3_BUCKET, key), e);
    }
  }

  public static void main(String[] args) {
    Timer timer = new Timer(false);
    timer.scheduleAtFixedRate(
        new CodekvastS3UploaderApplication(), 0L, SCAN_INTERVAL_SECONDS * 1000L);
  }
}
