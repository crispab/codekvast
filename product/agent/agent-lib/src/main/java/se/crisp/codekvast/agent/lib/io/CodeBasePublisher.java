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
package se.crisp.codekvast.agent.lib.io;

import se.crisp.codekvast.agent.lib.codebase.CodeBase;
import se.crisp.codekvast.agent.lib.codebase.CodeBaseFingerprint;

/**
 * Strategy for publishing a {@link se.crisp.codekvast.agent.lib.codebase.CodeBase}
 */
public interface CodeBasePublisher {

    /**
     * Is code base publishing enabled?
     *
     * @return true iff the codebase shall be scanned and uploaded
     */
    boolean isEnabled();

    /**
     * Checks whether the codebase needs to be published or not.
     *
     * @param fingerprint The Codebase fingerprint.
     * @return true iff the codebase needs to be published.
     * @throws CodekvastPublishingException when no contact with the receiver. Try again.
     */
    boolean needsToBePublished(CodeBaseFingerprint fingerprint) throws CodekvastPublishingException;

    /**
     * Publishes a codebase.
     *
     * @param codeBase The codebase to publish.
     * @throws CodekvastPublishingException when no contact with the consumer. Try again.
     */
    void publishCodebase(CodeBase codeBase) throws CodekvastPublishingException;

    /**
     * What is the nick-name of this publisher implementation.
     *
     * @return
     */
    String nickName();

    /**
     * Configure this publisher.
     *
     * @param configuration A semi-colon separated list of key:value pairs.
     */
    void configure(String configuration);

}
