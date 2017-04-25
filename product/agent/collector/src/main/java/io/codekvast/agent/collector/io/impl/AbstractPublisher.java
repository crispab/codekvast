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

import io.codekvast.agent.collector.io.Publisher;
import io.codekvast.agent.lib.config.CollectorConfig;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

/**
 * @author olle.hallin@crisp.se
 */
@Getter
public abstract class AbstractPublisher implements Publisher {

    private final CollectorConfig config;
    protected final Logger log;

    @Setter
    private boolean enabled;

    @Getter
    private int publicationCount;

    AbstractPublisher(Logger log, CollectorConfig config) {
        this.log = log;
        this.config = config;
    }

    @Override
    public void configure(String keyValuePairs) {
        String[] pairs = keyValuePairs.split(";");

        for (String pair : pairs) {
            if (!pair.trim().isEmpty()) {
                log.debug("Analyzing {}", pair);
                String[] parts = pair.trim().split("=");
                if (parts.length == 2) {
                    setValue(parts[0].trim(), parts[1].trim());
                } else {
                    log.warn("Illegal key-value pair: {}", pair);
                }
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

    void incrementPublicationCount() {
        publicationCount += 1;
    }

    abstract void doSetValue(String key, String value);
}
