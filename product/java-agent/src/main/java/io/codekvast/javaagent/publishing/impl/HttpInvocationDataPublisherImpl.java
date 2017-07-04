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
package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.model.v1.InvocationDataPublication;
import io.codekvast.javaagent.publishing.CodekvastPublishingException;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.javaagent.util.LogUtil;
import lombok.extern.java.Log;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static io.codekvast.javaagent.model.Endpoints.Agent.*;

/**
 * A HTTP implementation of InvocationDataPublisher.
 *
 * It uses the FileSystemInvocationDataPublisherImpl for creating a file, which then is POSTed to the server.
 *
 * @author olle.hallin@crisp.se
 */
@Log
public class HttpInvocationDataPublisherImpl extends AbstractInvocationDataPublisher {

    static final String NAME = "http";

    private static final MediaType APPLICATION_OCTET_STREAM = MediaType.parse("application/octet-stream");

    HttpInvocationDataPublisherImpl(AgentConfig config) {
        super(log, config);
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
    void doPublishInvocationData(long recordingIntervalStartedAtMillis, Set<String> invocations)
        throws CodekvastPublishingException {

        String url = getConfig().getInvocationDataUploadEndpoint();
        File file = null;
        try {
            InvocationDataPublication publication = createPublication(getCustomerId(), recordingIntervalStartedAtMillis, invocations);
            file = FileUtils.serializeToFile(publication,
                                             getConfig().getFilenamePrefix("invocations-"), ".ser");

            doPost(file, url, getCodeBaseFingerprint().getSha256(), publication.getInvocations().size());

            log.info(String.format("Codekvast uploaded %d invocations (%s) to %s", publication.getInvocations().size(),
                                    LogUtil.humanReadableByteCount(file.length()), url));
        } catch (Exception e) {
            throw new CodekvastPublishingException("Cannot upload invocation data to " + url, e);
        } finally {
            FileUtils.safeDelete(file);
        }

    }

    void doPost(File file, String url, String fingerprint, int publicationSize) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(PARAM_LICENSE_KEY, getConfig().getLicenseKey())
            .addFormDataPart(PARAM_FINGERPRINT, fingerprint)
            .addFormDataPart(PARAM_PUBLICATION_SIZE, String.valueOf(publicationSize))
            .addFormDataPart(PARAM_PUBLICATION_FILE, file.getName(),
                             RequestBody.create(APPLICATION_OCTET_STREAM, file))
            .build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

        try (Response response = executeRequest(request)) {
            if (!response.isSuccessful()) {
                throw new IOException(response.body().string());
            }
        }
    }

    // Make it possible to subclass in tests
    Response executeRequest(Request request) throws IOException {
        return getConfig().getHttpClient().newCall(request).execute();
    }

    private InvocationDataPublication createPublication(long customerId, long recordingIntervalStartedAtMillis, Set<String> invocations) {

        return InvocationDataPublication.builder()
                                        .commonData(getConfig().commonPublicationData().toBuilder()
                                                               .codeBaseFingerprint(getCodeBaseFingerprint().getSha256())
                                                               .customerId(customerId)
                                                               .sequenceNumber(this.getSequenceNumber())
                                                               .build())
                                        .recordingIntervalStartedAtMillis(recordingIntervalStartedAtMillis)
                                        .invocations(invocations)
                                        .build();
    }

}
