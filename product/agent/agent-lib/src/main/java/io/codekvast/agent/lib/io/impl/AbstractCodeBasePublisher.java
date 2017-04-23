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
package io.codekvast.agent.lib.io.impl;

import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.codekvast.agent.lib.io.CodeBasePublisher;

/**
 * Abstract base class for code base publishers.
 */
@Slf4j
@Getter
abstract class AbstractCodeBasePublisher implements CodeBasePublisher {

    private boolean enabled;
    private CodeBaseFingerprint fingerprint;

    @Override
    public void configure(String configuration) {
        log.debug("Received configuration: {}", configuration);
        String[] keyValuePairs = configuration.split(";");

        for (String pair : keyValuePairs) {
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
    public boolean needsToBePublished(CodeBaseFingerprint fingerprint) {
        return enabled && !fingerprint.equals(this.fingerprint);
    }

    @Override
    public void publishCodebase(CodeBase codeBase) {
        if (needsToBePublished(codeBase.getFingerprint())) {
            log.debug("Publishing codebase {}", codeBase.getFingerprint());

            doPublishCodeBase(codeBase);

            this.fingerprint = codeBase.getFingerprint();
        }
    }

    abstract void doSetValue(String key, String value);

    abstract void doPublishCodeBase(CodeBase codeBase);
}
