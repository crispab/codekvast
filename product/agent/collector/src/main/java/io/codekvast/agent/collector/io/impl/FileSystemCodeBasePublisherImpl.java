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
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.CodeBasePublication;
import io.codekvast.agent.lib.util.Constants;
import io.codekvast.agent.lib.util.FileUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dummy (no-op) implementation of CodeBasePublisher.
 */
@Slf4j
public class FileSystemCodeBasePublisherImpl extends AbstractCodeBasePublisher {

    public static final String NAME = "file-system";

    private String targetFile = "/tmp/codekvast/codebase-#date#.ser";

    FileSystemCodeBasePublisherImpl(CollectorConfig config) {
        super(log, config);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    boolean doSetValue(String key, String value) {
        if (key.equals("targetFile")) {
            this.targetFile = value;
            return true;
        }
        return false;
    }

    @Override
    public void doPublishCodeBase(CodeBase codeBase) throws CodekvastPublishingException {
        try {
            long startedAt = System.currentTimeMillis();
            File tempFile = File.createTempFile("codekvast", ".dat");
            @Cleanup ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
            CodeBasePublication publication = codeBase.getCodeBasePublication();

            oos.writeObject(publication);

            File expandedTargetFile = expandPlaceholders(new File(targetFile));
            mkdirs(expandedTargetFile);

            FileUtils.renameFile(tempFile, expandedTargetFile);
            log.debug("Published code base to {} in {} ms", expandedTargetFile, System.currentTimeMillis() - startedAt);
        } catch (IOException e) {
            throw new CodekvastPublishingException("Cannot publish code base", e);
        }
    }

    private void mkdirs(File expandedTargetFile) {
        File parentDir = expandedTargetFile.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
            if (!parentDir.isDirectory()) {
                log.warn("Cannot create {}", parentDir);
            }
        }
    }

    File expandPlaceholders(File file) {
        if (file == null) {
            return null;
        }

        String name = file.getName().replace("#hostname#", Constants.HOST_NAME).replace("#timestamp#", getTimestamp());

        File parentFile = file.getParentFile();
        return parentFile == null ? new File(name) : new File(parentFile, name);
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
    }
}
