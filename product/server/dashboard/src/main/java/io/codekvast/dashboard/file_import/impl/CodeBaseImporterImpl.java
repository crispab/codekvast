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

import io.codekvast.common.lock.Lock;
import io.codekvast.common.lock.LockTemplate;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.model.CodeBaseReceivedEvent;
import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.metrics.IntakeMetricsService;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import io.codekvast.javaagent.model.v3.CodeBasePublication3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static io.codekvast.dashboard.metrics.IntakeMetricsService.PublicationKind.CODEBASE;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CodeBaseImporterImpl implements CodeBaseImporter {

    private final CommonImporter commonImporter;
    private final ImportDAO importDAO;
    private final IntakeMetricsService metricsService;
    private final EventService eventService;
    private final LockTemplate lockTemplate;
    private final Clock clock;

    @Override
    @Transactional
    public boolean importPublication(CodeBasePublication3 publication) {
        logger.debug("Importing {}", publication);
        Instant startedAt = clock.instant();
        CommonPublicationData2 data = publication.getCommonData();

        lockTemplate.doWithLock(Lock.forCustomer(data.getCustomerId()), () -> {
            CommonImporter.ImportContext importContext = commonImporter.importCommonData(data);
            importDAO.importMethods(data, importContext, publication.getEntries());
        });

        eventService.send(CodeBaseReceivedEvent.builder()
                                               .customerId(data.getCustomerId())
                                               .appName(data.getAppName())
                                               .appVersion(data.getAppVersion())
                                               .agentVersion(data.getAgentVersion())
                                               .environment(data.getEnvironment())
                                               .hostname(data.getHostname())
                                               .size(publication.getEntries().size())
                                               .build());

        Duration duration = Duration.between(startedAt, clock.instant());
        logger.info("Imported {} in {}", publication, duration);
        metricsService.countImportedPublication(CODEBASE, publication.getEntries().size(), duration);
        return true;
    }
}
