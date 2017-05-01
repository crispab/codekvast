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
package io.codekvast.agent.lib.codebase;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.security.MessageDigest;
import java.util.Set;
import java.util.TreeSet;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

/**
 * An immutable fingerprint of a code base. Used for comparing different code bases for equality.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@RequiredArgsConstructor
@Slf4j
public class CodeBaseFingerprint {
    private final int numFiles;
    private String sha256;

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for incrementally building a CodeBaseFingerprint
     */
    static class Builder {
        Set<File> files = new TreeSet<File>();

        Builder record(File file) {
            if (files.add(file)) {
                log.trace("Recorded {}", file);
            } else {
                log.debug("Ignored duplicate file {}", file);
            }
            return this;
        }

        byte[] longToBytes(long l) {
            byte[] result = new byte[Long.SIZE / Byte.SIZE];
            for (int i = 7; i >= 0; i--) {
                result[i] = (byte) (l & 0xFF);
                l >>= Byte.SIZE;
            }
            return result;
        }

        @SneakyThrows
        CodeBaseFingerprint build() {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(longToBytes(files.size()));
            for (File file : files) {
                md.update(longToBytes(file.length()));
                md.update(longToBytes(file.lastModified()));
                md.update(file.getName().getBytes());
            }
            return new CodeBaseFingerprint(files.size(), printBase64Binary(md.digest()));
        }
    }
}
