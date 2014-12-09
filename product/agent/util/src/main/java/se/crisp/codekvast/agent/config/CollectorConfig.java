package se.crisp.codekvast.agent.config;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Builder;
import se.crisp.codekvast.agent.util.ConfigUtils;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 *
 * It also contains methods for reading and writing collector configuration files.
 *
 * @author Olle Hallin
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
    public static final int DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS = 600;
    public static final boolean DEFAULT_VERBOSE = false;
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    public static final String SAMPLE_CODEBASE_URI1 = "/path/to/codebase1/";
    public static final String SAMPLE_CODEBASE_URI2 = "/path/to/codebase2/";
    public static final String OVERRIDE_SEPARATOR = ";";
    public static final String UNSPECIFIED_VERSION = "unspecified";

    @NonNull
    private final SharedConfig sharedConfig;
    @NonNull
    private final String customerName;
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
    private final String codeBases;
    @NonNull
    private final String packagePrefixes;


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
        return new File(sharedConfig.getDataPath(), ConfigUtils.getNormalizedChildPath(customerName, appName));
    }

    public List<String> getNormalizedPackagePrefixes() {
        return ConfigUtils.getNormalizedPackagePrefixes(packagePrefixes);
    }

    public List<URI> getNormalizedCodeBaseUris() {
        return ConfigUtils.getNormalizedUriValues(codeBases, false);
    }

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Codekvast CollectorConfig");
    }

    public static CollectorConfig parseCollectorConfig(String args) throws URISyntaxException {
        String parts[] = args.split(";");
        URI uri = new URI(parts[0]);
        String[] overrides = new String[parts.length - 1];
        System.arraycopy(parts, 1, overrides, 0, overrides.length);
        return parseCollectorConfig(uri, overrides);
    }

    public static CollectorConfig parseCollectorConfig(URI uri, String... overrides) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            for (String override : overrides) {
                String parts[] = override.split("=");
                props.setProperty(parts[0], parts[1]);
            }

            return buildCollectorConfig(props);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", uri, e.getMessage()), e);
        }
    }

    public static CollectorConfig buildCollectorConfig(Properties props) {
        return CollectorConfig.builder()
                              .sharedConfig(SharedConfig.buildSharedConfig(props))
                              .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .customerName(ConfigUtils.getMandatoryStringValue(props, "customerName"))
                              .appName(ConfigUtils.getMandatoryStringValue(props, "appName"))
                              .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .codeBases(ConfigUtils.getMandatoryStringValue(props, "codeBases"))
                              .packagePrefixes(ConfigUtils.getMandatoryStringValue(props, "packagePrefixes"))
                              .collectorResolutionSeconds(ConfigUtils.getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                                          DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS))
                              .verbose(ConfigUtils.getOptionalBooleanValue(props, "verbose", DEFAULT_VERBOSE))
                              .clobberAopXml(ConfigUtils.getOptionalBooleanValue(props, "clobberAopXml", DEFAULT_CLOBBER_AOP_XML))
                              .methodExecutionPointcut(ConfigUtils.getOptionalStringValue(props, "methodExecutionPointcut",
                                                                                          DEFAULT_METHOD_EXECUTION_POINTCUT))
                              .build();
    }

    public static CollectorConfig createSampleCollectorConfig() {
        return CollectorConfig.builder()
                              .sharedConfig(SharedConfig.buildSampleSharedConfig())
                              .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                              .appName("Sample Application Name")
                              .appVersion(UNSPECIFIED_VERSION)
                              .codeBases(SAMPLE_CODEBASE_URI1 + " , " + SAMPLE_CODEBASE_URI2)
                              .packagePrefixes("com.acme. , foo.bar.")
                              .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS)
                              .verbose(DEFAULT_VERBOSE)
                              .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                              .methodExecutionPointcut(DEFAULT_METHOD_EXECUTION_POINTCUT)
                              .customerName("Customer Name")
                              .build();
    }

}
