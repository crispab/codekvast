package se.crisp.codekvast.agent.config;

import lombok.*;
import se.crisp.codekvast.agent.util.ConfigUtils;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 *
 * It also contains methods for reading and writing collector configuration files.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings({"UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectorConfig implements CodekvastConfig {
    public static final String INVOCATIONS_BASENAME = "invocations.dat";
    public static final String JVM_BASENAME = "jvm.dat";

    public static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    public static final String DEFAULT_ASPECTJ_OPTIONS = "";
    public static final String DEFAULT_METHOD_EXECUTION_POINTCUT = "public * *..*(..)";
    public static final int DEFAULT_COLLECTOR_RESOLUTION_SECONDS = 600;
    public static final boolean DEFAULT_VERBOSE = false;
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    public static final String SAMPLE_CODEBASE_URI1 = "/path/to/codebase1/";
    public static final String SAMPLE_CODEBASE_URI2 = "/path/to/codebase2/";
    private static final File SAMPLE_DATA_PATH = new File("/tmp)");
    public static final String SAMPLE_TAGS = "production, frontend-web";
    public static final String OVERRIDE_SEPARATOR = ";";
    public static final String UNSPECIFIED_VERSION = "unspecified";

    @NonNull
    private final File dataPath;
    private final String aspectjOptions;
    @NonNull
    private final String methodExecutionPointcut;
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

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Codekvast CollectorConfig");
    }

    public static CollectorConfig parseCollectorConfig(URI uri, String args) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            if (args != null) {
                String overrides[] = args.split(";");
                for (String override : overrides) {
                    String parts[] = override.split("=");
                    props.setProperty(parts[0], parts[1]);
                }
            }

            return buildCollectorConfig(props);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse " + uri, e);
        }
    }

    public static CollectorConfig buildCollectorConfig(Properties props) {
        return CollectorConfig.builder()
                              .appName(ConfigUtils.getMandatoryStringValue(props, "appName"))
                              .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .clobberAopXml(ConfigUtils.getOptionalBooleanValue(props, "clobberAopXml", DEFAULT_CLOBBER_AOP_XML))
                              .codeBase(ConfigUtils.getMandatoryStringValue(props, "codeBase"))
                              .collectorResolutionSeconds(ConfigUtils.getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                                          DEFAULT_COLLECTOR_RESOLUTION_SECONDS))
                              .dataPath(ConfigUtils.getDataPath(props))
                              .methodExecutionPointcut(ConfigUtils.getOptionalStringValue(props, "methodExecutionPointcut",
                                                                                          DEFAULT_METHOD_EXECUTION_POINTCUT))
                              .packagePrefixes(ConfigUtils.getMandatoryStringValue(props, "packagePrefixes"))
                              .tags(ConfigUtils.getOptionalStringValue(props, "tags", ""))
                              .verbose(ConfigUtils.getOptionalBooleanValue(props, "verbose", DEFAULT_VERBOSE))
                              .build();
    }

    public static CollectorConfig createSampleCollectorConfig() {
        return CollectorConfig.builder()
                              .appName("Sample Application Name")
                              .appVersion(UNSPECIFIED_VERSION)
                              .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                              .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                              .codeBase(SAMPLE_CODEBASE_URI1 + " , " + SAMPLE_CODEBASE_URI2)
                              .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_SECONDS)
                              .dataPath(SAMPLE_DATA_PATH)
                              .methodExecutionPointcut(DEFAULT_METHOD_EXECUTION_POINTCUT)
                              .packagePrefixes("com.acme. , foo.bar.")
                              .tags(SAMPLE_TAGS)
                              .verbose(DEFAULT_VERBOSE)
                              .build();
    }

}
