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
package io.codekvast.dashboard.metrics.impl;

import io.codekvast.dashboard.metrics.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {
    private static final String KIND_TAG = "kind";
    private static final String FORMAT_TAG = "format";
    private static final String CUSTOMER_ID_TAG = "cid";
    private static final String CUSTOMER_ENVIRONMENT_TAG = "cenv";

    private final MeterRegistry meterRegistry;

    @Override
    public void gaugePublicationQueueLength(int queueLength) {
        meterRegistry.gauge("codekvast.intake.queueLength", queueLength);
    }

    @Override
    public void countRejectedPublication() {
        meterRegistry.counter("codekvast.intake.rejected").increment();
    }

    @Override
    public void countImportedPublication(PublicationKind kind, String format) {
        String name = "codekvast.intake.accepted";
        meterRegistry.counter(name, KIND_TAG, asTag(kind), FORMAT_TAG, format).increment();
        meterRegistry.counter(name + "." + asTag(kind), FORMAT_TAG, format).increment();
    }

    @Override
    public void gaugePublicationSize(PublicationKind kind, long customerId, String customerEnvironment, int size) {
        String name = "codekvast.intake.publicationSize";

        meterRegistry.gauge(name,
                            asList(Tag.of(KIND_TAG, asTag(kind)),
                                   Tag.of(CUSTOMER_ID_TAG, Long.toString(customerId)),
                                   Tag.of(CUSTOMER_ENVIRONMENT_TAG, customerEnvironment)), size);

        meterRegistry.gauge(name + "." + asTag(kind),
                            asList(Tag.of(CUSTOMER_ID_TAG, Long.toString(customerId)),
                                   Tag.of(CUSTOMER_ENVIRONMENT_TAG, customerEnvironment)), size);
    }

    private String asTag(PublicationKind kind) {
        return kind.name().toLowerCase();
    }
}
