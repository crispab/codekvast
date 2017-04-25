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

import io.codekvast.agent.collector.io.CodekvastPublishingException;
import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.legacy.Jvm;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
@Getter
public abstract class AbstractInvocationDataPublisher extends AbstractPublisher implements InvocationDataPublisher {

    AbstractInvocationDataPublisher(Logger log, CollectorConfig config) {
        super(log, config);
    }

    @Override
    public void publishInvocationData(Jvm jvm, long recordingIntervalStartedAtMillis, Set<String> invocations)
        throws CodekvastPublishingException {
        if (isEnabled()) {
            incrementPublicationCount();

            log.debug("Publishing invocation data #{}", getPublicationCount());

            doPublishInvocationData(jvm, getPublicationCount(), recordingIntervalStartedAtMillis, invocations);
        }
    }

    abstract void doPublishInvocationData(Jvm jvm, int publishCount, long recordingIntervalStartedAtMillis,
                                          Set<String> invocations) throws CodekvastPublishingException;
}
