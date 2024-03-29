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
package io.codekvast.common.bootstrap;

import io.codekvast.common.logging.LoggingUtils;
import java.net.InetAddress;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** @author olle.hallin@crisp.se */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods", "OverlyComplexClass"})
@Component
@ConfigurationProperties(prefix = "codekvast.common")
@Validated
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"jwtSecret", "slackWebhookToken"})
@Slf4j
public class CodekvastCommonSettings {

  private final String hostname = getLocalHostname();
  private String applicationName;
  private String displayVersion;
  @Default private String environment = "dev";

  private String jwtSecret;

  @Default private Long jwtExpirationHours = 7 * 24L;

  private String slackWebhookToken;

  @Default private String slackWebhookUrl = "https://hooks.slack.com/services";

  /** The name of the person doing the last commit, injected from the build system. */
  private String committer;

  /** The date of the last commit, injected from the build system. */
  private String commitDate;

  /** The last commit message, injected from the build system. */
  private String commitMessage;

  /** What is the login base url? */
  @Default private String loginBaseUrl = "https://login.codekvast.io";

  /** What is the base URL of the Codekvast dashboard? */
  @Default private String dashboardBaseUrl = "https://dashboard.codekvast.io";

  /** What is the homepage base url? */
  @Default private String homepageBaseUrl = "https://www.codekvast.io";

  /** What is the support email? */
  @Default private String supportEmail = "support@codekvast.io";

  /** Where do the JVM put heap dumps on OutOfMemoryError? See also google-jib.xml */
  @Default private String heapDumpsPath = "/tmp/codekvast/heap-dumps";

  /** Which S3 bucket shall receive the heap dumps? */
  @Default private String heapDumpsBucket = "io.codekvast.heap-dumps";

  // For logging only
  @Default private Integer heapDumpUploaderDelaySeconds = 60;
  // For logging only
  @Default private Integer heapDumpUploaderIntervalSeconds = 60;

  @PostConstruct
  public void logStartup() {
    Runtime rt = Runtime.getRuntime();
    logger.info(
        "Runtime: Number of processors={}, free memory={}, total memory={}, max memory={}",
        rt.availableProcessors(),
        LoggingUtils.humanReadableByteCount(rt.freeMemory()),
        LoggingUtils.humanReadableByteCount(rt.totalMemory()),
        LoggingUtils.humanReadableByteCount(rt.maxMemory()));
    logger.info("{} started", this);
  }

  @PreDestroy
  public void logShutdown() {
    //noinspection UseOfSystemOutOrSystemErr
    System.out.printf("%s v%s (%s) shuts down%n", applicationName, displayVersion, environment);
    logger.info("{} shuts down", this);
  }

  @SneakyThrows
  private String getLocalHostname() {
    String hostName = InetAddress.getLocalHost().getHostName();
    int pos = hostName.indexOf('.');
    return hostName.substring(0, pos < 1 ? hostName.length() : pos);
  }
}
