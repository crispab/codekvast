/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.java.Log;

import java.io.File;
import java.nio.charset.Charset;
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
@Log
public class CodeBaseFingerprint {
    private final int numFiles;

    @NonNull
    private final String sha256;

    public static Builder builder(AgentConfig config) {
        return new Builder(config);
    }

    /**
     * Builder for incrementally building a CodeBaseFingerprint
     */
    @RequiredArgsConstructor
    public static class Builder {
        private final AgentConfig config;

        private final Set<File> files = new TreeSet<>();

        Builder record(File file) {
            if (files.add(file)) {
                logger.finest("Recorded " + file);
            } else {
                logger.fine("Ignored duplicate file " + file);
            }
            return this;
        }

        byte[] longToBytes(long l) {
            long value = l;
            byte[] result = new byte[Long.SIZE / Byte.SIZE];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) (value & 0xFF);
                value >>= Byte.SIZE;
            }
            return result;
        }

        @SneakyThrows
        public CodeBaseFingerprint build() {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            Charset utf8 = Charset.forName("UTF-8");
            md.update(config.getNormalizedPackages().toString().getBytes(utf8));
            md.update(config.getNormalizedExcludePackages().toString().getBytes(utf8));
            md.update(config.getMethodAnalyzer().toString().getBytes(utf8));

            md.update(longToBytes(files.size()));
            for (File file : files) {
                md.update(longToBytes(file.length()));
                md.update(longToBytes(file.lastModified()));
                md.update(file.getName().getBytes(utf8));
            }
            return new CodeBaseFingerprint(files.size(), printBase64Binary(md.digest()));
        }
    }
}
