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
package se.crisp.codekvast.agent.daemon.codebase;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static java.lang.Math.max;

/**
 * An immutable fingerprint of a code base. Used for comparing different code bases for equality.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class CodeBaseFingerprint {
    private final int count;
    private final long size;
    private final long lastModified;
    private final int cachedHashCode;

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for incrementally building a CodeBaseFingerprint
     */
    static class Builder {
        private int count;
        private long size;
        private long lastModified;
        private long hashCodeSum;

        public Builder record(File file) {
            count += 1;
            size += file.length();
            lastModified = max(lastModified, file.lastModified());
            hashCodeSum += file.hashCode();
            log.trace("Recorded {}, {}", file, this);
            return this;
        }

        CodeBaseFingerprint build() {
            return new CodeBaseFingerprint(count, size, lastModified, Long.valueOf(hashCodeSum).hashCode());
        }
    }
}
