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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** @author olle.hallin@crisp.se */
@Service
@RequiredArgsConstructor
public class PublicationMetricsServiceImpl implements PublicationMetricsService {

  private final MeterRegistry meterRegistry;
  private final AtomicInteger queueLengthGauge = new AtomicInteger(0);
  private final Map<PublicationType, AtomicInteger> publicationSizeGauges = new HashMap<>();
  private final Map<PublicationType, AtomicInteger> ignoredSyntheticSignaturesGauges =
      new HashMap<>();

  @PostConstruct
  void createGauges() {
    meterRegistry.gauge("codekvast.publication.queueLength", queueLengthGauge);
    for (PublicationType type : PublicationType.values()) {
      Tags tags = getTags(type);

      AtomicInteger size = new AtomicInteger(0);
      publicationSizeGauges.put(type, size);
      meterRegistry.gauge("codekvast.publication.size", tags, size);

      AtomicInteger ignoredSynthetic = new AtomicInteger(0);
      ignoredSyntheticSignaturesGauges.put(type, ignoredSynthetic);
      meterRegistry.gauge("codekvast.publication.synthetic", tags, ignoredSynthetic);
    }
  }

  @Override
  public void gaugePublicationQueueLength(int queueLength) {
    this.queueLengthGauge.set(queueLength);
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
    publicationSizeGauges.get(type).set(size);
    ignoredSyntheticSignaturesGauges.get(type).set(ignoredSyntheticSignatures);
    meterRegistry.counter("codekvast.publication.accepted", tags).increment();
    meterRegistry.timer("codekvast.publication.import.duration", tags).record(duration);
  }

  private Tags getTags(PublicationType type) {
    return Tags.of("type", type.toString());
  }
}
