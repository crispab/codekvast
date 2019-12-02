/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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

import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.InvocationDataReceivedEvent;
import io.codekvast.dashboard.file_import.InvocationDataImporter;
import io.codekvast.dashboard.file_import.impl.CommonImporter.ImportContext;
import io.codekvast.dashboard.metrics.IntakeMetricsService;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v2.InvocationDataPublication2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.TreeSet;

import static io.codekvast.dashboard.metrics.IntakeMetricsService.PublicationKind.INVOCATIONS;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InvocationDataImporterImpl implements InvocationDataImporter {

    private final CommonImporter commonImporter;
    private final ImportDAO importDAO;
    private final IntakeMetricsService metricsService;
    private final EventService eventService;

    @Override
    @Transactional
    public boolean importPublication(InvocationDataPublication2 publication) {
        Instant startedAt = Instant.now();
        logger.debug("Importing {}", publication);

        CommonPublicationData2 data = publication.getCommonData();
        ImportContext importContext = commonImporter.importCommonData(data);
        importDAO.importInvocations(importContext, publication.getRecordingIntervalStartedAtMillis(),
                                    new TreeSet<>(publication.getInvocations()));

        eventService.send(InvocationDataReceivedEvent.builder()
                                                     .customerId(data.getCustomerId())
                                                     .appName(data.getAppName())
                                                     .appVersion(data.getAppVersion())
                                                     .agentVersion(data.getAgentVersion())
                                                     .environment(data.getEnvironment())
                                                     .hostname(data.getHostname())
                                                     .size(publication.getInvocations().size())
                                                     .build());

        Duration duration = Duration.between(startedAt, Instant.now());
        logger.info("Imported {} in {}", publication, duration);
        metricsService.countImportedPublication(INVOCATIONS, publication.getInvocations().size(), duration);
        return true;
    }
}
