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
package io.codekvast.dashboard.metrics.impl;

import io.codekvast.dashboard.metrics.PublicationMetricsService;
import io.codekvast.dashboard.model.PublicationType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** @author olle.hallin@crisp.se */
@Service
@RequiredArgsConstructor
public class PublicationMetricsServiceImpl implements PublicationMetricsService {

  private final MeterRegistry meterRegistry;

  @Override
  public void gaugePublicationQueueLength(int queueLength) {
    meterRegistry.gauge("codekvast.publication.queueLength", queueLength);
  }

  @Override
  public void countRejectedPublication(PublicationType type) {
    Tags tags = getTags(type);
    meterRegistry.counter("codekvast.publication.rejected", tags).increment();
  }

  @Override
  public void recordImportedPublication(
      PublicationType type, int size, int ignoredSyntheticSignatures, Duration duration) {
    Tags tags = getTags(type);
    meterRegistry.counter("codekvast.publication.accepted", tags).increment();
    meterRegistry.gauge("codekvast.publication.size", tags, size);
    meterRegistry.gauge("codekvast.publication.synthetic", tags, ignoredSyntheticSignatures);
    meterRegistry.timer("codekvast.publication.imported_in.millis", tags).record(duration);
  }

  private Tags getTags(PublicationType type) {
    return Tags.of("type", type.toString());
  }
}
