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
package io.codekvast.agent.collector.io;

import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.model.v1.legacy.Jvm;

import java.util.Set;

/**
 * Strategy for publishing collected invocation data.
 *
 * @author olle.hallin@crisp.se
 */
public interface InvocationDataPublisher {

    /**
     * What is the name of the publishing strategy?
     *
     * @return The name of the strategy.
     */
    String getName();

    /**
     * Configure this publisher.
     *
     * @param keyValuePairs The specialized config received from the server, a semi-colon separated list of key=value pairs.
     */
    void configure(String keyValuePairs);

    /**
     * Associate this published with a certain code base.
     *
     * @param fingerprint The fingerprint of the executing code base.
     */
    void setCodeBaseFingerprint(CodeBaseFingerprint fingerprint);

    /**
     * Publish the invocation data.
     *
     * @param recordingIntervalStartedAtMillis When the recording of these invocations were started.
     * @param invocations                      The set of invocations to publish.
     * @throws CodekvastPublishingException when publishing fails.
     */
    void publishInvocationData(long recordingIntervalStartedAtMillis, Set<String> invocations)
        throws CodekvastPublishingException;
}
