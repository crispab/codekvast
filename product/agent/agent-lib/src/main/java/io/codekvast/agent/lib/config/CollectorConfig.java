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
package io.codekvast.agent.lib.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.codekvast.agent.lib.appversion.AppVersionResolver;
import io.codekvast.agent.lib.model.Endpoints;
import lombok.*;
import io.codekvast.agent.lib.util.ConfigUtils;
import okhttp3.OkHttpClient;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class CollectorConfig implements CodekvastConfig {
    public static final String INVOCATIONS_BASENAME = "invocations.dat";
    public static final String JVM_BASENAME = "jvm.dat";

    @NonNull
    private String licenseKey;

    @NonNull
    private String serverUrl;

    @NonNull
    private File dataPath;

    @NonNull
    private String aspectjOptions;

    private boolean bridgeAspectjMessagesToSLF4J;

    @NonNull
    private String methodVisibility;

    private int collectorResolutionSeconds;

    private boolean clobberAopXml;

    @NonNull
    private String appName;

    @NonNull
    private String appVersion;

    @NonNull
    private String codeBase;

    @NonNull
    private String packages;

    @NonNull
    private String excludePackages;

    @NonNull
    private String tags;

    @JsonIgnore
    private transient String resolvedAppVersion;

    @JsonIgnore
    private transient OkHttpClient httpClient;

    @JsonIgnore
    public File getAspectFile() {
        return new File(myDataPath(appName), "aop.xml");
    }

    @JsonIgnore
    public File getSignatureFile(String appName) {
        return new File(myDataPath(appName), "signatures.dat");
    }

    @JsonIgnore
    protected File myDataPath(String appName) {
        return new File(dataPath, ConfigUtils.normalizePathName(appName));
    }

    @JsonIgnore
    public List<String> getNormalizedPackages() {
        return ConfigUtils.getNormalizedPackages(packages);
    }

    @JsonIgnore
    public List<String> getNormalizedExcludePackages() {
        return ConfigUtils.getNormalizedPackages(excludePackages);
    }

    @JsonIgnore
    public List<File> getCodeBaseFiles() {
        return ConfigUtils.getCommaSeparatedFileValues(codeBase, false);
    }

    @JsonIgnore
    public MethodAnalyzer getMethodAnalyzer() {
        return new MethodAnalyzer(this.methodVisibility);
    }

    @JsonIgnore
    public String getPollConfigRequestEndpoint() {
        return String.format("%s%s", serverUrl, Endpoints.AGENT_V1_POLL_CONFIG);
    }

    @JsonIgnore
    public String getCodeBaseUploadEndpoint() {
        return String.format("%s%s", serverUrl, Endpoints.AGENT_V1_UPLOAD_CODEBASE);
    }

    @JsonIgnore
    public String getInvocationDataUploadEndpoint() {
        return String.format("%s%s", serverUrl, Endpoints.AGENT_V1_UPLOAD_INVOCATION_DATA);
    }

    @JsonIgnore
    public String getResolvedAppVersion() {
        if (resolvedAppVersion == null) {
            resolvedAppVersion = new AppVersionResolver(this).resolveAppVersion();
        }
        return resolvedAppVersion;
    }

    @JsonIgnore
    public OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                // TODO: make OkHttpClient timeouts configurable
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                // TODO: OkHttpClient.proxy()
                .build();
        }
        return httpClient;
    }

    public String getFilenamePrefix(@NonNull String prefix) {
        String result = String.format("%s-%s-%s-", prefix.replaceAll("-+$", ""), appName, getResolvedAppVersion());
        return result.toLowerCase().replaceAll("[^a-z0-9._+-]", "");
    }
}
