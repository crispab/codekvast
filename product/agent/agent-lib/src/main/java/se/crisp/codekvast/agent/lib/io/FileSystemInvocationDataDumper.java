/**
 * Copyright (c) 2015-2016 Crisp AB
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

import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.model.Jvm;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

/**
 * Implementation for dumping collected invocation data to the local file system.
 *
 * @author olle.hallin@crisp.se
 */
public class FileSystemInvocationDataDumper implements InvocationDataDumper {

    private final CollectorConfig config;
    private final PrintStream out;
    private final File jvmFile;

    public FileSystemInvocationDataDumper(CollectorConfig config, PrintStream out) {
        this.config = config;
        this.out = out;
        this.jvmFile = config == null ? null : config.getJvmFile();
    }

    @Override
    public boolean prepareForDump() {
        File outputPath = config.getInvocationsFile().getParentFile();
        outputPath.mkdirs();
        return outputPath.exists();
    }

    @Override
    public void dumpData(Jvm jvm, int dumpCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        dumpJvmData(jvm);
        dumpInvocationData(dumpCount, recordingIntervalStartedAtMillis, invocations);
    }

    private void dumpJvmData(Jvm jvm) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", jvmFile.getParentFile());
            jvm.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, jvmFile);
        } catch (IOException e) {
            out.println("Codekvast cannot save " + jvmFile + ": " + e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }

    }

    private void dumpInvocationData(int dumpCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        FileUtils.writeInvocationDataTo(config.getInvocationsFile(), dumpCount, recordingIntervalStartedAtMillis,
                                        invocations);

    }

}
