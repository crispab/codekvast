/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.file_import.impl;

import io.codekvast.dashboard.file_import.CodeBaseImporter;
import io.codekvast.dashboard.metrics.DashboardMetricsService;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static io.codekvast.dashboard.metrics.DashboardMetricsService.PublicationKind.CODEBASE;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CodeBaseImporterImpl implements CodeBaseImporter {

    private final ImportDAO importDAO;
    private final DashboardMetricsService metricsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importPublication(CodeBasePublication2 publication) {
        logger.info("Importing {}", publication);

        CommonPublicationData2 data = publication.getCommonData();
        long customerId = data.getCustomerId();
        long appId = importDAO.importApplication(data);
        long environmentId = importDAO.importEnvironment(data);
        long jvmId = importDAO.importJvm(data, appId, environmentId);
        importDAO.importMethods(data, customerId, appId, environmentId, jvmId, publication.getCommonData().getPublishedAtMillis(),
                                publication.getEntries());
        metricsService.gaugePublicationSize(CODEBASE, customerId, publication.getCommonData().getEnvironment(), publication.getEntries().size());
        return true;
    }
}
