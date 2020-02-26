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
package io.codekvast.javaagent.model.v2;

import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * A validated parameter object for getting config from the Codekvast Service.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder(toBuilder = true)
public class GetConfigRequest2 {

  /** What is my license key? Blank is acceptable and means run with a trial license. */
  @NonNull private final String licenseKey;

  /** What is my app's name? */
  @NonNull
  @Size(min = 1, message = "appName must be at least 1 characters")
  private final String appName;

  /** What is my app's version? */
  @NonNull
  @Size(min = 1, message = "appVersion must be at least 1 characters")
  private final String appVersion;

  /** What is my environment? */
  @NonNull
  @Size(min = 1, message = "environment must be at least 1 characters")
  private final String environment;

  /** Which version of the agent is doing this request? */
  @NonNull
  @Size(min = 1, message = "agentVersion must be at least 1 characters")
  private final String agentVersion;

  /** What is the name of the host in which the agent executes? */
  @NonNull
  @Size(min = 1, message = "hostname must be at least 1 characters")
  private final String hostname;

  /** What is the random UUID of the JVM in which the agent executes? */
  @NonNull
  @Size(min = 1, message = "jvmUuid must be at least 1 characters")
  private final String jvmUuid;

  /** When was the JVM in which the agent executes started? */
  private final long startedAtMillis;

  /** What is the ID of the computer in which the agent executes? */
  @NonNull
  @Size(min = 1, message = "computerId must be at least 1 characters")
  private final String computerId;

  public static GetConfigRequest2 sample() {
    return GetConfigRequest2.builder()
        .appName("appName")
        .appVersion("appVersion")
        .agentVersion("agentVersion")
        .environment("environment")
        .computerId("computerId")
        .hostname("hostname")
        .jvmUuid("jvmUuid")
        .licenseKey("licenseKey")
        .build();
  }

  public static GetConfigRequest2 fromFormat1(GetConfigRequest1 req, String environment) {
    return GetConfigRequest2.builder()
        .appName(req.getAppName())
        .appVersion(req.getAppVersion())
        .agentVersion(req.getAgentVersion())
        .environment(environment)
        .computerId(req.getComputerId())
        .hostname(req.getHostname())
        .jvmUuid(req.getJvmUuid())
        .licenseKey(req.getLicenseKey())
        .build();
  }
}
