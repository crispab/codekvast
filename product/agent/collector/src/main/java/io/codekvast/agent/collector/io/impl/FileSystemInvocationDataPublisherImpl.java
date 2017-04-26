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

import io.codekvast.agent.collector.io.CodekvastPublishingException;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.InvocationDataPublication;
import io.codekvast.agent.lib.util.Constants;
import io.codekvast.agent.lib.util.FileUtils;
import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Set;

/**
 * Implementation for publishing collected invocation data to files in the local file system.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class FileSystemInvocationDataPublisherImpl extends AbstractInvocationDataPublisher {

    public static final String NAME = "file-system";

    @Setter
    private String targetFile = "/tmp/codekvast/invocations-#timestamp#.ser";

    public FileSystemInvocationDataPublisherImpl(CollectorConfig config) {
        super(log, config);
        super.setEnabled(true);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    boolean doSetValue(String key, String value) {
        if (key.equals("targetFile")) {
            setTargetFile(value);
            return true;
        }
        return false;
    }

    @Override
    void doPublishInvocationData(long recordingIntervalStartedAtMillis,
                                 Set<String> invocations)
        throws CodekvastPublishingException {

        try {
            long startedAt = System.currentTimeMillis();
            File tempFile = File.createTempFile("codekvast-invocations-", ".tmp");
            @Cleanup ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));

            InvocationDataPublication publication = createPublication(recordingIntervalStartedAtMillis, invocations);

            oos.writeObject(publication);

            File expandedTargetFile = FileUtils.expandPlaceholders(new File(targetFile));
            FileUtils.mkdirsFor(expandedTargetFile);
            FileUtils.renameFile(tempFile, expandedTargetFile);
            log.debug("Published invocation data to {} in {} ms", expandedTargetFile, System.currentTimeMillis() - startedAt);
        } catch (IOException e) {
            throw new CodekvastPublishingException("Cannot publish invocation data", e);
        }
    }

    private InvocationDataPublication createPublication(long recordingIntervalStartedAtMillis, Set<String> invocations) {

        return InvocationDataPublication.builder()
                                        .appName(getConfig().getAppName())
                                        .appVersion(getConfig().getResolvedAppVersion())
                                        .codeBaseFingerprint(getCodeBaseFingerprint().getSha256())
                                        .collectorVersion(Constants.COLLECTOR_VERSION)
                                        .computerId(Constants.COMPUTER_ID)
                                        .hostName(Constants.HOST_NAME)
                                        .invocations(invocations)
                                        .jvmUuid(Constants.JVM_UUID)
                                        .publicationCount(getPublicationCount())
                                        .publishedAtMillis(System.currentTimeMillis())
                                        .recordingIntervalStartedAtMillis(recordingIntervalStartedAtMillis)
                                        .build();
    }

}
