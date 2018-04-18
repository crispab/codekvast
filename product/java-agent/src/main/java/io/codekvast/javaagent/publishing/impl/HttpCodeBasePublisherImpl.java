/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.codebase.CodeBase;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.model.v2.CodeBasePublication2;
import io.codekvast.javaagent.publishing.CodekvastPublishingException;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.javaagent.util.LogUtil;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;

/**
 * A HTTP implementation of CodeBasePublisher.
 *
 * It uses the FileSystemCodeBasePublisherImpl for creating a file, which then is POSTed to the server.
 *
 * @author olle.hallin@crisp.se
 */
@Log
public class HttpCodeBasePublisherImpl extends AbstractCodeBasePublisher {

    static final String NAME = "http";

    HttpCodeBasePublisherImpl(AgentConfig config) {
        super(logger, config);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void doPublishCodeBase(CodeBase codeBase) throws CodekvastPublishingException {
        String url = getConfig().getCodeBaseUploadEndpoint();

        File file = null;
        try {

            CodeBasePublication2 publication = codeBase.getCodeBasePublication(getCustomerId(), this.getSequenceNumber());
            file = FileUtils.serializeToFile(publication, getConfig().getFilenamePrefix("codebase-"), ".ser");

            doPost(file, url, codeBase.getFingerprint().toString(), publication.getEntries().size());

            logger.fine(String.format("Codekvast uploaded %d methods (%s) to %s", publication.getEntries().size(),
                                    LogUtil.humanReadableByteCount(file.length()), url));
        } catch (IOException e) {
            throw new CodekvastPublishingException("Cannot upload code base to " + url, e);
        } finally {
            FileUtils.safeDelete(file);
        }
    }

}
