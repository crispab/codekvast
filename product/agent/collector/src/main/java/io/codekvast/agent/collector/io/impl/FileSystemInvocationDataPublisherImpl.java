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

import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import io.codekvast.agent.lib.model.Jvm;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Implementation for publishing collected invocation data to files in the local file system.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class FileSystemInvocationDataPublisherImpl implements InvocationDataPublisher {

    private final CollectorConfig config;
    private final File jvmFile;

    public FileSystemInvocationDataPublisherImpl(CollectorConfig config) {
        this.config = config;
        this.jvmFile = config.getJvmFile();
    }

    @Override
    public boolean prepareForPublish() {
        File outputPath = config.getInvocationsFile().getParentFile();
        outputPath.mkdirs();
        return outputPath.exists();
    }

    @Override
    public void publishData(Jvm jvm, int publishCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        publishJvmData(jvm);
        publishInvocationData(publishCount, recordingIntervalStartedAtMillis, invocations);
    }

    private void publishJvmData(Jvm jvm) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", jvmFile.getParentFile());
            jvm.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, jvmFile);
        } catch (IOException e) {
            log.debug("Codekvast cannot save {}: {}", jvmFile, e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }

    }

    private void publishInvocationData(int publishCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        FileUtils.writeInvocationDataTo(config.getInvocationsFile(), publishCount, recordingIntervalStartedAtMillis,
                                        invocations);

    }

}
