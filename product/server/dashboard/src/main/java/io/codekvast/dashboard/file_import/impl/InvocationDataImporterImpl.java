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
package io.codekvast.dashboard.file_import.impl;

import static io.codekvast.common.util.LoggingUtils.humanReadableDuration;

import io.codekvast.common.aspects.Restartable;
import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.InvocationDataReceivedEvent;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.impl.CommonImporter.ImportContext;
import io.codekvast.dashboard.metrics.AgentMetricsService;
import io.codekvast.dashboard.model.PublicationType;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** @author olle.hallin@crisp.se */
@Component
@Slf4j
@RequiredArgsConstructor
public class InvocationDataImporterImpl implements InvocationDataImporter {

  private final CommonImporter commonImporter;
  private final ImportDAO importDAO;
  private final SyntheticSignatureService syntheticSignatureService;
  private final AgentMetricsService metricsService;
  private final EventService eventService;
  private final LockTemplate lockTemplate;
  private final Clock clock;

  @Override
  @Transactional(rollbackFor = Exception.class)
  @Restartable
  public boolean importPublication(InvocationDataPublication2 publication) throws Exception {
    logger.debug("Importing {}", publication);

    CommonPublicationData2 data = publication.getCommonData();

    Set<String> invocations =
        publication.getInvocations().stream()
            .filter(i -> !syntheticSignatureService.isSyntheticMethod(i))
            .collect(Collectors.toSet());

    int ignoredSyntheticSignatures = publication.getInvocations().size() - invocations.size();

    Duration duration =
        lockTemplate.doWithLockOrThrow(
            Lock.forCustomer(data.getCustomerId()),
            () ->
                doImportInvocations(
                    publication.getRecordingIntervalStartedAtMillis(), data, invocations));

    logger.info(
        "Imported {} in {} (ignoring {} synthetic signatures)",
        publication,
        humanReadableDuration(duration),
        ignoredSyntheticSignatures);

    metricsService.recordImportedPublication(
        PublicationType.INVOCATIONS, invocations.size(), ignoredSyntheticSignatures, duration);

    return true;
  }

  private Duration doImportInvocations(
      long recordingIntervalStartedAtMillis, CommonPublicationData2 data, Set<String> invocations) {
    Instant startedAt = clock.instant();

    ImportContext importContext = commonImporter.importCommonData(data);

    importDAO.importInvocations(importContext, recordingIntervalStartedAtMillis, invocations);

    eventService.send(
        InvocationDataReceivedEvent.builder()
            .customerId(data.getCustomerId())
            .appName(data.getAppName())
            .appVersion(data.getAppVersion())
            .agentVersion(data.getAgentVersion())
            .environment(data.getEnvironment())
            .hostname(data.getHostname())
            .size(invocations.size())
            .receivedAt(Instant.ofEpochMilli(data.getPublishedAtMillis()))
            .trialPeriodEndsAt(importContext.getTrialPeriodEndsAt())
            .build());

    return Duration.between(startedAt, clock.instant());
  }
}
