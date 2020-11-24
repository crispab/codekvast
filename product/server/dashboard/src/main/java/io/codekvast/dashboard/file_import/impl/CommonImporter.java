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

import io.codekvast.common.customer.CustomerService;
import io.codekvast.javaagent.model.v2.CommonPublicationData2;
import java.time.Instant;
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
  private final CustomerService customerService;

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
        .trialPeriodEndsAt(customerService.getCustomerDataByCustomerId(data.getCustomerId())
            .getTrialPeriodEndsAt())
        .build();
  }

  @Value
  @Builder
  static class ImportContext {
    @NonNull Long customerId;
    @NonNull Long appId;
    @NonNull Long environmentId;
    @NonNull Long jvmId;
    @NonNull Long publishedAtMillis;
    Instant trialPeriodEndsAt;
  }
}
