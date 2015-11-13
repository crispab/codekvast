package se.crisp.codekvast.agent.lib.config;

import se.crisp.codekvast.agent.lib.util.ConfigUtils;
import se.crisp.codekvast.agent.lib.util.FileUtils;
import se.crisp.codekvast.agent.lib.util.SignatureUtils;

import java.io.File;
import java.net.URI;
import java.util.Properties;

/**
 * A factory for {@link CollectorConfig} objects.
 */
public class CollectorConfigFactory {

    public static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    public static final String DEFAULT_ASPECTJ_OPTIONS = "";
    public static final String DEFAULT_METHOD_VISIBILITY = SignatureUtils.PUBLIC;
    public static final int DEFAULT_COLLECTOR_RESOLUTION_SECONDS = 600;
    public static final boolean DEFAULT_VERBOSE = false;
    public static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    public static final String SAMPLE_CODEBASE_URI1 = "/path/to/codebase1/";
    public static final String SAMPLE_CODEBASE_URI2 = "/path/to/codebase2/";
    public static final File SAMPLE_DATA_PATH = new File("/tmp/codekvast");
    public static final String SAMPLE_TAGS = "key1=value1, key2=value2";
    public static final String OVERRIDE_SEPARATOR = ";";
    public static final String UNSPECIFIED_VERSION = "unspecified";

    private static final String TAGS_KEY = "tags";

    private CollectorConfigFactory() {
    }

    public static CollectorConfig parseCollectorConfig(URI uri, String cmdLineArgs) {
        return parseCollectorConfig(uri, cmdLineArgs, false);
    }

    public static CollectorConfig parseCollectorConfig(URI uri, String cmdLineArgs, boolean prependSystemPropertiesToTags) {
        if (uri == null) {
            return null;
        }

        try {
            Properties props = FileUtils.readPropertiesFrom(uri);

            parseOverrides(props, System.getProperty(CollectorConfigLocator.SYSPROP_OPTS));
            parseOverrides(props, cmdLineArgs);
            if (prependSystemPropertiesToTags) {
                doPrependSystemPropertiesToTags(props);
            }

            return buildCollectorConfig(props);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse " + uri, e);
        }
    }

    static void parseOverrides(Properties props, String args) {
        if (args != null) {
            String overrides[] = args.split(OVERRIDE_SEPARATOR);
            for (String override : overrides) {
                String parts[] = override.split("=");
                props.setProperty(parts[0].trim(), parts[1].trim());
            }
        }
    }

    public static CollectorConfig buildCollectorConfig(Properties props) {
        return CollectorConfig.builder()
                              .appName(validateAppName(ConfigUtils.getMandatoryStringValue(props, "appName")))
                              .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED_VERSION))
                              .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .clobberAopXml(ConfigUtils.getOptionalBooleanValue(props, "clobberAopXml", DEFAULT_CLOBBER_AOP_XML))
                              .codeBase(ConfigUtils.getMandatoryStringValue(props, "codeBase"))
                              .collectorResolutionSeconds(ConfigUtils.getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                                          DEFAULT_COLLECTOR_RESOLUTION_SECONDS))
                              .dataPath(ConfigUtils.getDataPath(props))
                              .methodVisibility(ConfigUtils.getOptionalStringValue(props, "methodVisibility",
                                                                                   DEFAULT_METHOD_VISIBILITY))
                              .packagePrefixes(ConfigUtils.getMandatoryStringValue(props, "packagePrefixes"))
                              .tags(ConfigUtils.getOptionalStringValue(props, TAGS_KEY, ""))
                              .verbose(getVerboseValue(props))
                              .build();
    }

    private static void doPrependSystemPropertiesToTags(Properties props) {
        String systemPropertiesTags = createSystemPropertiesTags();

        String oldTags = props.getProperty(TAGS_KEY);
        if (oldTags != null) {
            props.setProperty(TAGS_KEY, systemPropertiesTags + ", " + oldTags);
        } else {
            props.setProperty(TAGS_KEY, systemPropertiesTags);
        }
    }

    private static String createSystemPropertiesTags() {
        String[] sysProps = {
                "java.runtime.name",
                "java.runtime.version",
                "os.arch",
                "os.name",
                "os.version",
        };

        StringBuilder sb = new StringBuilder();
        String delimiter = "";

        for (String prop : sysProps) {
            String v = System.getProperty(prop);
            if (v != null && !v.isEmpty()) {
                sb.append(delimiter).append(prop).append("=").append(v.replaceAll(",", "\\,"));
                delimiter = ", ";
            }
        }

        return sb.toString();
    }

    private static String validateAppName(String appName) {
        if (appName.startsWith(".")) {
            throw new IllegalArgumentException("appName must not start with '.': " + appName);
        }
        return appName;
    }

    public static void saveTo(CollectorConfig config, File file) {
        FileUtils.writePropertiesTo(file, config, "Codekvast CollectorConfig");
    }

    /**
     * @return true if the system property {@code codekvast.options} contains "verbose=true" or the environment variable {@code
     * CODEKVAST_VERBOSE} is defined,
     */
    public static boolean isSyspropVerbose() {
        Properties props = new Properties();
        String overrides = System.getProperty(CollectorConfigLocator.SYSPROP_OPTS);

        String verbose = System.getenv(CollectorConfigLocator.ENVVAR_VERBOSE) != null ? "verbose=true" : null;
        if (verbose != null) {
            overrides = overrides == null ? verbose : overrides + ";" + verbose;
        }
        parseOverrides(props, overrides);
        return getVerboseValue(props);
    }

    private static boolean getVerboseValue(Properties props) {
        return ConfigUtils.getOptionalBooleanValue(props, "verbose", DEFAULT_VERBOSE);
    }

    public static CollectorConfig createSampleCollectorConfig() {
        return CollectorConfigFactory.builder()
                                     .appName("Sample Application Name")
                                     .codeBase(SAMPLE_CODEBASE_URI1 + " , " + SAMPLE_CODEBASE_URI2)
                                     .packagePrefixes("com.acme. , foo.bar.")
                                     .dataPath(SAMPLE_DATA_PATH)
                                     .build();
    }

    public static CollectorConfig.CollectorConfigBuilder builder() {
        return CollectorConfig.builder()
                              .appVersion(UNSPECIFIED_VERSION)
                              .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                              .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                              .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_SECONDS)
                              .dataPath(ConfigUtils.getDataPath(new Properties()))
                              .methodVisibility(DEFAULT_METHOD_VISIBILITY)
                              .tags(createSystemPropertiesTags() + ", " + SAMPLE_TAGS)
                              .verbose(DEFAULT_VERBOSE);
    }

}
