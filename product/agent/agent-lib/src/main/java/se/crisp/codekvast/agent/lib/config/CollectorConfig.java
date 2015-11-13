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
    private String packagePrefixes;
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
    public List<String> getNormalizedPackagePrefixes() {
        return ConfigUtils.getNormalizedPackagePrefixes(packagePrefixes);
    }

    @JsonIgnore
    public List<File> getCodeBaseFiles() {
        return ConfigUtils.getCommaSeparatedFileValues(codeBase, false);
    }

    @JsonIgnore
    public MethodFilter getMethodFilter() {
        return new MethodFilter(this.methodVisibility);
    }

}
