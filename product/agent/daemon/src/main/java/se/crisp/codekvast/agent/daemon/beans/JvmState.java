/**
 * Copyright (c) 2015 Crisp AB
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
package se.crisp.codekvast.agent.daemon.beans;

import lombok.Data;
import se.crisp.codekvast.agent.daemon.codebase.CodeBase;
import se.crisp.codekvast.agent.lib.model.Jvm;

import java.io.File;
import java.time.Instant;

/**
 * Mutable state for a {@link Jvm} object.
 */
@Data
public class JvmState {
    private Jvm jvm;
    private File invocationsFile;
    private CodeBase codeBase;
    private String appVersion;
    private Instant jvmDataProcessedAt = Instant.MIN;
    private boolean firstRun = true;
    private long databaseAppId;
    private long databaseJvmId;

    public Instant getJvmDumpedAt() {
        return Instant.ofEpochMilli(jvm.getDumpedAtMillis());
    }

    public Instant getJvmStartedAt() {
        return Instant.ofEpochMilli(jvm.getStartedAtMillis());
    }
}
