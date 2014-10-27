package se.crisp.codekvast.agent.util;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Builder;

import java.io.File;
import java.net.URI;
import java.util.Properties;

/**
 * Encapsulates the configuration that is used by codekvast-collector.
 * <p/>
 * It also contains methods for reading and writing collector configuration files.
 *
 * @author Olle Hallin
 */
@SuppressWarnings({"UnusedDeclaration", "ClassWithTooManyFields", "ClassWithTooManyMethods"})
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectorConfig {
    public static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    public static final boolean DEFAULT_INVOKE_ASPECTJ_WEAVER = true;
    public static final String DEFAULT_ASPECTJ_OPTIONS = "";
    public static final int DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS = 600;
    public static final boolean DEFAULT_VERBOSE = false;
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";

    @NonNull
    private final SharedConfig sharedConfig;
    @NonNull
    private final String aspectjOptions;
    private final int collectorResolutionSeconds;
    private final boolean clobberAopXml;
    private final boolean verbose;
    private final boolean invokeAspectjWeaver;

    public File getAspectFile() {
        return new File(sharedConfig.myDataPath(), "aop.xml");
    }

    public File getCollectorLogFile() {
        return new File(sharedConfig.myDataPath(), "codekvast-collector.log");
    }

    public File getJvmRunFile() {
        return sharedConfig.getJvmRunFile();
    }

    public File getUsageFile() {
        return sharedConfig.getUsageFile();
    }


    public String getPackagePrefix() {
        return sharedConfig.getPackagePrefix();
    }

    public String getNormalizedPackagePrefix() {
        return sharedConfig.getNormalizedPackagePrefix();
    }

    public void saveTo(File file) {
        FileUtils.writePropertiesTo(file, this, "Codekvast CollectorConfig");
    }

    public static CollectorConfig parseConfigFile(String file) {
        return parseCollectorConfigFile(new File(file).toURI());
    }

    public static CollectorConfig parseCollectorConfigFile(URI uri) {
        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            return CollectorConfig.builder()
                                  .sharedConfig(SharedConfig.buildSharedConfig(props))
                                  .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                                  .collectorResolutionSeconds(ConfigUtils.getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                                              DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS))
                                  .verbose(Boolean.parseBoolean(
                                          ConfigUtils.getOptionalStringValue(props, "verbose", Boolean.toString(DEFAULT_VERBOSE))))
                                  .clobberAopXml(Boolean.parseBoolean(ConfigUtils.getOptionalStringValue(props, "clobberAopXml",
                                                                                                         Boolean.toString(
                                                                                                                 DEFAULT_CLOBBER_AOP_XML))))
                                  .invokeAspectjWeaver(Boolean.parseBoolean(ConfigUtils.getOptionalStringValue(props, "invokeAspectjWeaver",
                                                                                                               Boolean.toString(
                                                                                                                       DEFAULT_INVOKE_ASPECTJ_WEAVER))))
                                  .build();
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Cannot parse %s: %s", uri, e.getMessage()), e);
        }
    }

    public static CollectorConfig createSampleCollectorConfig() {
        return CollectorConfig.builder()
                              .sharedConfig(SharedConfig.buildSampleSharedConfig())
                              .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                              .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_INTERVAL_SECONDS)
                              .verbose(DEFAULT_VERBOSE)
                              .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                              .invokeAspectjWeaver(DEFAULT_INVOKE_ASPECTJ_WEAVER)
                              .build();
    }
}
