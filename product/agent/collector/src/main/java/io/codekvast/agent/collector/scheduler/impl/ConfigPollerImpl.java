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
import io.codekvast.agent.lib.appversion.AppVersionResolver;
import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.model.v1.rest.GetConfigRequest1;
import io.codekvast.agent.lib.model.v1.rest.GetConfigResponse1;
import io.codekvast.agent.lib.util.ComputerID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class ConfigPollerImpl implements ConfigPoller {
    private final CollectorConfig config;
    private final GetConfigRequest1 requestTemplate;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AppVersionResolver appVersionResolver = new AppVersionResolver();

    @Getter
    private CodeBaseFingerprint codeBaseFingerprint;

    public ConfigPollerImpl(CollectorConfig config) {
        this.config = config;
        this.requestTemplate = GetConfigRequest1.builder()
                                                .appName(config.getAppName())
                                                .appVersion("to-be-expanded")
                                                .collectorVersion(getCollectorVersion())
                                                .computerId(ComputerID.compute().toString())
                                                .hostName(getHostName())
                                                .jvmUuid(UUID.randomUUID().toString())
                                                .licenseKey(config.getLicenseKey())
                                                .startedAtMillis(System.currentTimeMillis())
                                                .build();

        this.httpClient = buildHttpClient(config);
    }

    private OkHttpClient buildHttpClient(CollectorConfig config) {
        // TODO: pick values from config

        return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // TODO: .proxy()
            .build();
    }

    @Override
    public GetConfigResponse1 doPoll(boolean firstTime) throws Exception {
        this.codeBaseFingerprint = calculateCodeBaseFingerprint(firstTime);

        GetConfigRequest1 request = expandRequestTemplate();

        log.debug("Posting {} to {}", request, config.getConfigRequestEndpoint());

        GetConfigResponse1 response =
            objectMapper.readValue(doHttpPost(objectMapper.writeValueAsString(request)), GetConfigResponse1.class);

        log.debug("Received {} in response", response);
        return response;
    }

    private GetConfigRequest1 expandRequestTemplate() {
        GetConfigRequest1.GetConfigRequest1Builder builder = requestTemplate
            .toBuilder()
            .appVersion(appVersionResolver.resolveAppVersion(config));

        if (codeBaseFingerprint != null) {
            builder.codeBaseFingerprint(codeBaseFingerprint.getSha256());
        }
        return builder.build();
    }

    private CodeBaseFingerprint calculateCodeBaseFingerprint(boolean firstTime) {
        return firstTime ? new CodeBase(config).getFingerprint() : null;
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private String getCollectorVersion() {
        return ConfigPollerImpl.class.getPackage().getImplementationVersion();
    }

    private String doHttpPost(String bodyJson) throws IOException {

        Request request = new Request.Builder()
            .url(config.getConfigRequestEndpoint())
            .post(RequestBody.create(JSON, bodyJson))
            .build();

        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException(response.body().string());
        }

        return response.body().string();
    }
}
