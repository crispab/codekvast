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
 * Strategy for dumping a {@link se.crisp.codekvast.agent.lib.codebase.CodeBase}
 */
public interface CodebaseDumper {

    /**
     * Checks whether the codebase needs to be dumped or not.
     *
     * @param fingerprint The Codebase fingerprint.
     * @return true iff the codebase needs to be dumped.
     * @throws CodebaseDumpException when no contact with the receiver. Try again.
     */
    boolean needsToBeDumped(CodeBaseFingerprint fingerprint) throws CodebaseDumpException;

    /**
     * Dumps a codebase.
     *
     * @param codeBase The codebase to dump.
     * @throws CodebaseDumpException when no contact with the receiver. Try again.
     */
    void dumpCodebase(CodeBase codeBase) throws CodebaseDumpException;

}
