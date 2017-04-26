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
import io.codekvast.agent.lib.model.Endpoints;
import io.codekvast.agent.lib.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;

/**
 * A HTTP implementation of CodeBasePublisher.
 *
 * It uses the FileSystemCodeBasePublisherImpl for creating a file, which then is POSTed to the server.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class HttpCodeBasePublisherImpl extends AbstractCodeBasePublisher {

    public static final String NAME = "http";

    private static final MediaType APPLICATION_OCTET_STREAM = MediaType.parse("application/octet-stream");

    private FileSystemCodeBasePublisherImpl fileSystemPublisher;

    HttpCodeBasePublisherImpl(CollectorConfig config) {
        super(log, config);
        this.fileSystemPublisher = new FileSystemCodeBasePublisherImpl(config);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    boolean doSetValue(String key, String value) {
        return false;
    }

    @Override
    public void doPublishCodeBase(CodeBase codeBase) throws CodekvastPublishingException {
        String url = getConfig().getCodeBaseUploadEndpoint();
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast-codebase-", ".ser");
            fileSystemPublisher.setTargetFile(tmpFile.getAbsolutePath());
            fileSystemPublisher.doPublishCodeBase(codeBase);

            doPost(tmpFile);

        } catch (Exception e) {
            throw new CodekvastPublishingException("Cannot upload code base to " + url, e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }
    }

    void doPost(File file) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(Endpoints.AGENT_V1_LICENSE_KEY_PARAM, getConfig().getLicenseKey())
            .addFormDataPart(Endpoints.AGENT_V1_PUBLICATION_FILE_PARAM, file.getName(),
                             RequestBody.create(APPLICATION_OCTET_STREAM, file))
            .build();

        Request request = new Request.Builder()
            .url(getConfig().getCodeBaseUploadEndpoint())
            .post(requestBody)
            .build();

        Response response = getConfig().getHttpClient().newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }
    }
}
