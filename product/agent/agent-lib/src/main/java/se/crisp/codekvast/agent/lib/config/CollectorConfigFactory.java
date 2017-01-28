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

    private static final boolean DEFAULT_CLOBBER_AOP_XML = true;
    private static final String DEFAULT_ASPECTJ_OPTIONS = "";
    private static final String DEFAULT_METHOD_VISIBILITY = SignatureUtils.PROTECTED;
    private static final int DEFAULT_COLLECTOR_RESOLUTION_SECONDS = 600;
    private static final boolean DEFAULT_VERBOSE = false;
    private static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    private static final String SAMPLE_CODEBASE_URI1 = "/path/to/codebase1/";
    private static final String SAMPLE_CODEBASE_URI2 = "/path/to/codebase2/";
    private static final File DEFAULT_DATA_PATH = new File("/tmp/codekvast/.collector");
    private static final String SAMPLE_TAGS = "key1=value1, key2=value2";
    private static final String OVERRIDE_SEPARATOR = ";";
    private static final String UNSPECIFIED = "unspecified";
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

    private static void parseOverrides(Properties props, String args) {
        if (args != null) {
            String overrides[] = args.split(OVERRIDE_SEPARATOR);
            for (String override : overrides) {
                String parts[] = override.split("=");
                props.setProperty(parts[0].trim(), parts[1].trim());
            }
        }
    }

    public static CollectorConfig buildCollectorConfig(Properties props) {
        // Backwards compatibility kludge. "packages" and "packagePrefixes" were renamed in version 0.16.0
        String packages = ConfigUtils.getOptionalStringValue(props, "packagePrefixes", null);
        if (packages == null) {
            packages = ConfigUtils.getMandatoryStringValue(props, "packages");
        }

        String excludePackages = ConfigUtils.getOptionalStringValue(props, "excludePackagePrefixes", null);
        if (excludePackages == null) {
            excludePackages = ConfigUtils.getOptionalStringValue(props, "excludePackages", "");
        }
        // End

        return CollectorConfig.builder()
                              .appName(validateAppName(ConfigUtils.getMandatoryStringValue(props, "appName")))
                              .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED))
                              .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                              .clobberAopXml(ConfigUtils.getOptionalBooleanValue(props, "clobberAopXml", DEFAULT_CLOBBER_AOP_XML))
                              .codeBase(ConfigUtils.getMandatoryStringValue(props, "codeBase"))
                              .collectorResolutionSeconds(ConfigUtils.getOptionalIntValue(props, "collectorResolutionSeconds",
                                                                                          DEFAULT_COLLECTOR_RESOLUTION_SECONDS))
                              .dataPath(ConfigUtils.getDataPath(props, DEFAULT_DATA_PATH))
                              .methodVisibility(ConfigUtils.getOptionalStringValue(props, "methodVisibility",
                                                                                   DEFAULT_METHOD_VISIBILITY))
                              .packages(packages)
                              .excludePackages(excludePackages)
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
        return CollectorConfigFactory.createTemplateConfig().toBuilder()
                                     .appName("Sample Application Name")
                                     .codeBase(SAMPLE_CODEBASE_URI1 + " , " + SAMPLE_CODEBASE_URI2)
                                     .packages("com.acme. , foo.bar.")
                                     .excludePackages("some.excluded.package")
                                     .dataPath(DEFAULT_DATA_PATH)
                                     .build();
    }

    public static CollectorConfig createTemplateConfig() {
        return CollectorConfig.builder()
                              .appName(UNSPECIFIED)
                              .appVersion(UNSPECIFIED)
                              .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                              .clobberAopXml(DEFAULT_CLOBBER_AOP_XML)
                              .codeBase(UNSPECIFIED)
                              .collectorResolutionSeconds(DEFAULT_COLLECTOR_RESOLUTION_SECONDS)
                              .dataPath(ConfigUtils.getDataPath(new Properties(), DEFAULT_DATA_PATH))
                              .methodVisibility(DEFAULT_METHOD_VISIBILITY)
                              .packages(UNSPECIFIED)
                              .excludePackages("")
                              .tags(createSystemPropertiesTags() + ", " + SAMPLE_TAGS)
                              .verbose(DEFAULT_VERBOSE)
                              .build();
    }

}
