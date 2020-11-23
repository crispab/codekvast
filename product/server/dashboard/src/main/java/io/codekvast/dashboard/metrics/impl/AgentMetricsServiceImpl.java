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

import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.dashboard.metrics.AgentStatistics;
import io.codekvast.dashboard.model.PublicationType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** @author olle.hallin@crisp.se */
@Service
@RequiredArgsConstructor
public class AgentMetricsServiceImpl implements AgentMetricsService {

  private final MeterRegistry meterRegistry;
  private final AtomicInteger queueLengthGauge = new AtomicInteger(0);
  private final AtomicInteger disabledAgentsGauge = new AtomicInteger(0);
  private final AtomicInteger deadAgentsGauge = new AtomicInteger(0);
  private final AtomicInteger aliveAgentsGauge = new AtomicInteger(0);
  private final Map<PublicationType, AtomicInteger> publicationLogicalSizeGauges = new HashMap<>();
  private final Map<PublicationType, AtomicLong> publicationPhysicalSizeGauges = new HashMap<>();
  private final Map<PublicationType, AtomicInteger> ignoredSyntheticSignaturesGauges =
      new HashMap<>();

  @PostConstruct
  void createGauges() {
    meterRegistry.gauge("codekvast.publication.queueLength", queueLengthGauge);
    meterRegistry.gauge("codekvast.agents", Tags.of("state", "disabled"), disabledAgentsGauge);
    meterRegistry.gauge("codekvast.agents", Tags.of("state", "dead"), deadAgentsGauge);
    meterRegistry.gauge("codekvast.agents", Tags.of("state", "alive"), aliveAgentsGauge);

    for (PublicationType type : PublicationType.values()) {
      Tags tags = getTags(type);

      AtomicInteger logicalSize = new AtomicInteger(0);
      publicationLogicalSizeGauges.put(type, logicalSize);
      meterRegistry.gauge("codekvast.publication.size.entries", tags, logicalSize);

      AtomicLong physicalSize = new AtomicLong(0L);
      publicationPhysicalSizeGauges.put(type, physicalSize);
      meterRegistry.gauge("codekvast.publication.size.bytes", tags, physicalSize);

      AtomicInteger ignoredSynthetic = new AtomicInteger(0);
      ignoredSyntheticSignaturesGauges.put(type, ignoredSynthetic);
      meterRegistry.gauge("codekvast.publication.synthetic", tags, ignoredSynthetic);
    }
  }

  @Override
  public void gaugeAgents(AgentStatistics statistics) {
    this.disabledAgentsGauge.set(statistics.getNumDisabled());
    this.deadAgentsGauge.set(statistics.getNumDead());
    this.aliveAgentsGauge.set(statistics.getNumAlive());
  }

  @Override
  public void gaugePublicationQueueLength(int queueLength) {
    this.queueLengthGauge.set(queueLength);
  }

  @Override
  public void gaugePhysicalPublicationSize(PublicationType type, long sizeInBytes) {
    this.publicationPhysicalSizeGauges.get(type).set(sizeInBytes);
  }

  @Override
  public void recordImportedPublication(
      PublicationType type, int logicalSize, int ignoredSyntheticSignatures, Duration duration) {
    Tags tags = getTags(type);
    publicationLogicalSizeGauges.get(type).set(logicalSize);
    ignoredSyntheticSignaturesGauges.get(type).set(ignoredSyntheticSignatures);
    meterRegistry.counter("codekvast.publication.accepted", tags).increment();
    meterRegistry.timer("codekvast.publication.import.duration", tags).record(duration);
  }

  @Override
  public void countAgentPoll() {
    meterRegistry.counter("codekvast.agent.polls").increment();
  }

  @Override
  public void countWeededRows(int deletedRows) {
    meterRegistry.counter("codekvast.weeder.deleted_rows").increment(deletedRows);
  }

  private Tags getTags(PublicationType type) {
    return Tags.of("type", type.toString());
  }
}
