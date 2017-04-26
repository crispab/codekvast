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
package io.codekvast.agent.collector.scheduler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codekvast.agent.collector.scheduler.ConfigPoller;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;
import io.codekvast.agent.lib.util.Constants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class ConfigPollerImpl implements ConfigPoller {
    private final CollectorConfig config;
    private final GetConfigRequest1 requestTemplate;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    private CodeBaseFingerprint codeBaseFingerprint;

    public ConfigPollerImpl(CollectorConfig config) {
        this.config = config;
        this.requestTemplate = GetConfigRequest1.builder()
                                                .appName(config.getAppName())
                                                .appVersion("to-be-resolved")
                                                .collectorVersion(Constants.COLLECTOR_VERSION)
                                                .computerId(Constants.COMPUTER_ID)
                                                .hostName(Constants.HOST_NAME)
                                                .jvmUuid(Constants.JVM_UUID)
                                                .licenseKey(config.getLicenseKey())
                                                .startedAtMillis(System.currentTimeMillis())
                                                .build();
    }

    @Override
    public GetConfigResponse1 doPoll(boolean firstTime) throws Exception {
        this.codeBaseFingerprint = calculateCodeBaseFingerprint(firstTime);

        GetConfigRequest1 request = expandRequestTemplate();

        log.debug("Posting {} to {}", request, config.getPollConfigRequestEndpoint());

        GetConfigResponse1 response =
            objectMapper.readValue(doHttpPost(objectMapper.writeValueAsString(request)), GetConfigResponse1.class);

        log.debug("Received {} in response", response);
        return response;
    }

    private GetConfigRequest1 expandRequestTemplate() {
        GetConfigRequest1.GetConfigRequest1Builder builder = requestTemplate
            .toBuilder()
            .appVersion(config.getResolvedAppVersion());

        if (codeBaseFingerprint != null) {
            builder.codeBaseFingerprint(codeBaseFingerprint.getSha256());
        }
        return builder.build();
    }

    private CodeBaseFingerprint calculateCodeBaseFingerprint(boolean firstTime) {
        return firstTime ? new CodeBase(config).getFingerprint() : null;
    }

    private String doHttpPost(String bodyJson) throws IOException {

        Request request = new Request.Builder()
            .url(config.getPollConfigRequestEndpoint())
            .post(RequestBody.create(JSON, bodyJson))
            .build();

        Response response = config.getHttpClient().newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }

        return response.body().string();
    }
}
