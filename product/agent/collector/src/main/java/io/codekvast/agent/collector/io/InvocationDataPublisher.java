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

import io.codekvast.agent.lib.model.Jvm;

import java.util.Set;

/**
 * Strategy for publishing collected invocation data.
 *
 * @author olle.hallin@crisp.se
 */
public interface InvocationDataPublisher {

    /**
     * Make preparations for publishing data..
     *
     * @return true iff the preparation was successful.
     */
    boolean prepareForPublish();

    /**
     * Publish the data.
     *
     * @param jvm                              The JVM data.
     * @param publishCount                     The publishing counter.
     * @param recordingIntervalStartedAtMillis When the recording of these invocations were started.
     * @param invocations                      The set of invocations to publish.
     */
    void publishData(Jvm jvm, int publishCount, long recordingIntervalStartedAtMillis, Set<String> invocations);
}
