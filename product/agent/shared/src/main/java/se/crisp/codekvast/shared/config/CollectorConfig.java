package se.crisp.codekvast.shared.config;

import lombok.*;
import se.crisp.codekvast.shared.util.ConfigUtils;

import java.io.File;
import java.util.List;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 *
 * @author olle.hallin@crisp.se
 */
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectorConfig implements CodekvastConfig {
    public static final String INVOCATIONS_BASENAME = "invocations.dat";
    public static final String JVM_BASENAME = "jvm.dat";

    @NonNull
    private final File dataPath;
    @NonNull
    private final String aspectjOptions;
    @NonNull
    private final String methodVisibility;
    private final int collectorResolutionSeconds;
    private final boolean clobberAopXml;
    private final boolean verbose;
    @NonNull
    private final String appName;
    @NonNull
    private final String appVersion;
    @NonNull
    private final String codeBase;
    @NonNull
    private final String packagePrefixes;
    @NonNull
    private final String tags;

    public File getAspectFile() {
        return new File(myDataPath(appName), "aop.xml");
    }

    public File getJvmFile() {
        return new File(myDataPath(appName), JVM_BASENAME);
    }

    public File getCollectorLogFile() {
        return new File(myDataPath(appName), "codekvast-collector.log");
    }

    public File getInvocationsFile() {
        return new File(myDataPath(appName), INVOCATIONS_BASENAME);
    }

    public File getSignatureFile(String appName) {
        return new File(myDataPath(appName), "signatures.dat");
    }

    protected File myDataPath(String appName) {
        return new File(dataPath, ConfigUtils.normalizePathName(appName));
    }

    public List<String> getNormalizedPackagePrefixes() {
        return ConfigUtils.getNormalizedPackagePrefixes(packagePrefixes);
    }

    public List<File> getCodeBaseFiles() {
        return ConfigUtils.getCommaSeparatedFileValues(codeBase, false);
    }

    public MethodFilter getMethodVisibility() {
        return new MethodFilter(this.methodVisibility);
    }

}
