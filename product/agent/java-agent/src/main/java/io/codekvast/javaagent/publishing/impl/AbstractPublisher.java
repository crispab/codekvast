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
package io.codekvast.javaagent.publishing.impl;

import static io.codekvast.javaagent.model.Endpoints.Agent.PARAM_FINGERPRINT;
import static io.codekvast.javaagent.model.Endpoints.Agent.PARAM_LICENSE_KEY;
import static io.codekvast.javaagent.model.Endpoints.Agent.PARAM_PUBLICATION_FILE;
import static io.codekvast.javaagent.model.Endpoints.Agent.PARAM_PUBLICATION_SIZE;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.publishing.Publisher;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Abstract base class for publishers.
 *
 * @author olle.hallin@crisp.se
 */
@Getter
public abstract class AbstractPublisher implements Publisher {

  private static final MediaType APPLICATION_OCTET_STREAM =
      MediaType.parse("application/octet-stream");
  private final AgentConfig config;
  protected final Logger logger;

  @Setter private boolean enabled;

  private long customerId = -1L;

  private int sequenceNumber;

  AbstractPublisher(Logger logger, AgentConfig config) {
    this.logger = logger;
    this.config = config;
  }

  @Override
  public void configure(long customerId, String keyValuePairs) {
    if (customerId != this.customerId) {
      this.customerId = customerId;
      logger.fine("Using customerId " + customerId);
    }

    String[] pairs = keyValuePairs.split(";");

    for (String pair : pairs) {
      pair = pair.trim();
      if (!pair.isEmpty()) {
        logger.finest("Analyzing " + pair);
        String[] parts = pair.split("=");
        if (parts.length == 2) {
          setValue(parts[0].trim(), parts[1].trim());
        } else {
          logger.warning("Illegal key-value pair: " + pair);
        }
      }
    }
  }

  private void setValue(String key, String value) {
    if (key.equals("enabled")) {
      boolean newValue = Boolean.valueOf(value);
      boolean oldValue = this.enabled;
      if (oldValue != newValue) {
        logger.fine(String.format("Setting %s=%s, was=%s", key, newValue, this.enabled));
        this.enabled = newValue;
      }
    } else {
      boolean recognized = doSetValue(key, value);
      if (recognized) {
        logger.fine(String.format("Setting %s=%s", key, value));
      } else {
        logger.warning(String.format("Unrecognized key-value pair: %s=%s", key, value));
      }
    }
  }

  void incrementSequenceNumber() {
    sequenceNumber += 1;
  }

  /**
   * Override in concrete subclasses to handle private configuration settings.
   *
   * @param key The name of the parameter.
   * @param value The value of the parameter.
   * @return true iff the key was recognized.
   */
  @SuppressWarnings("SameReturnValue")
  boolean doSetValue(String key, String value) {
    return false;
  }

  void doPost(File file, String url, String fingerprint, int publicationSize) throws IOException {
    RequestBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(PARAM_LICENSE_KEY, getConfig().getLicenseKey())
            .addFormDataPart(PARAM_FINGERPRINT, fingerprint)
            .addFormDataPart(PARAM_PUBLICATION_SIZE, String.valueOf(publicationSize))
            .addFormDataPart(
                PARAM_PUBLICATION_FILE,
                file.getName(),
                RequestBody.create(APPLICATION_OCTET_STREAM, file))
            .build();

    Request request = new Request.Builder().url(url).post(requestBody).build();
    try (Response response = executeRequest(request)) {
      if (!response.isSuccessful()) {
        throw new IOException(response.body().string());
      }
    }
  }

  // Make it simple to subclass and override in tests...
  Response executeRequest(Request request) throws IOException {
    return getConfig().getHttpClient().newCall(request).execute();
  }
}
