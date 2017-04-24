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
package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.collector.io.CodeBasePublisher;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.codebase.CodeBaseScanner;
import io.codekvast.agent.lib.config.CollectorConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for code base publishers.
 */
@Slf4j
@Getter
abstract class AbstractCodeBasePublisher implements CodeBasePublisher {

    private final CollectorConfig config;
    private boolean enabled;

    @Setter
    private CodeBaseFingerprint codeBaseFingerprint;

    AbstractCodeBasePublisher(CollectorConfig config) {
        this.config = config;
    }

    @Override
    public void configure(String keyValuePairs) {
        String[] pairs = keyValuePairs.split(";");

        for (String pair : pairs) {
            log.debug("Analyzing {}", pair);
            String[] parts = pair.trim().split("=");
            if (parts.length == 2) {
                setValue(parts[0].trim(), parts[1].trim());
            } else {
                log.warn("Illegal key-value pair: {}", pair);
            }
        }
    }

    private void setValue(String key, String value) {
        if (key.equals("enabled")) {
            boolean newValue = Boolean.valueOf(value);
            log.debug("Setting enabled={}, was={}", newValue, this.enabled);
            this.enabled = newValue;
        } else {
            doSetValue(key, value);
        }
    }

    @Override
    public void publishCodebase() {
        if (enabled) {
            CodeBase newCodeBase = new CodeBase(config);
            if (!newCodeBase.getFingerprint().equals(codeBaseFingerprint)) {
                new CodeBaseScanner().scanSignatures(newCodeBase);

                doPublishCodeBase(newCodeBase);

                codeBaseFingerprint = newCodeBase.getFingerprint();
            }
        }
    }

    abstract void doSetValue(String key, String value);

    abstract void doPublishCodeBase(CodeBase codeBase);
}
