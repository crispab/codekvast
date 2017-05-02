/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.file_import.impl;

import io.codekvast.agent.api.model.v1.CommonPublicationData;
import io.codekvast.agent.api.model.v1.InvocationDataPublication;
import io.codekvast.warehouse.file_import.InvocationDataImporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.TreeSet;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class InvocationDataImporterImpl implements InvocationDataImporter {
    private final ImportDAO importDAO;

    @Inject
    public InvocationDataImporterImpl(ImportDAO importDAO) {
        this.importDAO = importDAO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPublication(InvocationDataPublication publication) {
        log.debug("Importing {}", publication);

        CommonPublicationData commonData = publication.getCommonData();
        long appId = importDAO.importApplication(commonData);
        long jvmId = importDAO.importJvm(commonData);
        importDAO.importInvocations(appId, jvmId, publication.getCommonData().getPublishedAtMillis(),
                                    new TreeSet<>(publication.getInvocations()));
    }
}
