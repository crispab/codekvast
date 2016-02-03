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
package se.crisp.codekvast.agent.lib.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import se.crisp.codekvast.agent.lib.util.ConfigUtils;

import java.io.File;
import java.util.List;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 *
 * @author olle.hallin@crisp.se
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Builder
public class CollectorConfig implements CodekvastConfig {
    public static final String INVOCATIONS_BASENAME = "invocations.dat";
    public static final String JVM_BASENAME = "jvm.dat";

    @NonNull
    private File dataPath;
    @NonNull
    private String aspectjOptions;
    @NonNull
    private String methodVisibility;
    private int collectorResolutionSeconds;
    private boolean clobberAopXml;
    private boolean verbose;
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
    public File getAspectFile() {
        return new File(myDataPath(appName), "aop.xml");
    }

    @JsonIgnore
    public File getJvmFile() {
        return new File(myDataPath(appName), JVM_BASENAME);
    }

    @JsonIgnore
    public File getCollectorLogFile() {
        return new File(myDataPath(appName), "codekvast-collector.log");
    }

    @JsonIgnore
    public File getInvocationsFile() {
        return new File(myDataPath(appName), INVOCATIONS_BASENAME);
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

}
