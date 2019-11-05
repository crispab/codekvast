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

import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;

/**
 * Helper for importing common stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
class CommonImporter {

    private final ImportDAO importDAO;

    ImportContext importCommonData(CommonPublicationData2 data) {
        long appId = importDAO.importApplication(data);
        long environmentId = importDAO.importEnvironment(data);
        long jvmId = importDAO.importJvm(data, appId, environmentId);

        return ImportContext.builder()
                            .customerId(data.getCustomerId())
                            .appId(appId)
                            .environmentId(environmentId)
                            .jvmId(jvmId)
                            .publishedAtMillis(data.getPublishedAtMillis())
                            .build();
    }

    @Value
    @Builder
    static class ImportContext {
        @NonNull
        private final Long customerId;

        @NonNull
        private final Long appId;

        @NonNull
        private final Long environmentId;

        @NonNull
        private final Long jvmId;

        @NonNull
        private final Long publishedAtMillis;
    }
}
