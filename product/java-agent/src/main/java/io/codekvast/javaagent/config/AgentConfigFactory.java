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
package io.codekvast.javaagent.config;

import io.codekvast.javaagent.util.ConfigUtils;
import io.codekvast.javaagent.util.FileUtils;
import io.codekvast.javaagent.util.SignatureUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * A factory for {@link AgentConfig} objects.
 */
public class AgentConfigFactory {

    private static final boolean DEFAULT_BRIDGE_ASPECTJ_LOGGING_TO_SLF4J = true;
    private static final String DEFAULT_ASPECTJ_OPTIONS = "";
    private static final String DEFAULT_ENVIRONMENT = "";
    private static final String DEFAULT_METHOD_VISIBILITY = SignatureUtils.PROTECTED;
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    private static final String DEFAULT_HTTP_PROXY_HOST = null;
    private static final int DEFAULT_HTTP_PROXY_PORT = 3128;
    private static final int DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_HTTP_READ_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_HTTP_WRITE_TIMEOUT_SECONDS = 30;
    private static final String SAMPLE_ASPECTJ_OPTIONS = "-verbose -showWeaveInfo";
    private static final String SAMPLE_CODEBASE_URI1 = "/path/to/codebase1/";
    private static final String SAMPLE_CODEBASE_URI2 = "/path/to/codebase2/";
    private static final String SAMPLE_TAGS = "key1=value1, key2=value2";
    private static final String OVERRIDE_SEPARATOR = ";";
    private static final String UNSPECIFIED = "unspecified";
    private static final String TAGS_KEY = "tags";
    private static final String TRIAL_LICENSE_KEY = "";
    private static final File DEFAULT_ASPECT_FILE;

    static {
        try {
            DEFAULT_ASPECT_FILE = File.createTempFile("codekvast-", "-aop.xml");
            DEFAULT_ASPECT_FILE.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create codekvast-aop.xml", e);
        }
    }

    private AgentConfigFactory() {
    }

    static AgentConfig parseAgentConfig(File file, String cmdLineArgs) {
        return parseAgentConfig(file, cmdLineArgs, false);
    }

    public static AgentConfig parseAgentConfig(File file, String cmdLineArgs, boolean prependSystemPropertiesToTags) {
        if (file == null) {
            return null;
        }

        try {
            Properties props = FileUtils.readPropertiesFrom(file);

            parseOverrides(props, System.getProperty(AgentConfigLocator.SYSPROP_OPTS));
            parseOverrides(props, cmdLineArgs);
            if (prependSystemPropertiesToTags) {
                doPrependSystemPropertiesToTags(props);
            }

            return buildAgentConfig(props);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse " + file, e);
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

    private static AgentConfig buildAgentConfig(Properties props) {

        return AgentConfig.builder()
                          .appName(validateAppName(ConfigUtils.getMandatoryStringValue(props, "appName")))
                          .appVersion(ConfigUtils.getOptionalStringValue(props, "appVersion", UNSPECIFIED))
                          .aspectFile(DEFAULT_ASPECT_FILE)
                          .aspectjOptions(ConfigUtils.getOptionalStringValue(props, "aspectjOptions", DEFAULT_ASPECTJ_OPTIONS))
                          .bridgeAspectjMessagesToSLF4J(
                                           ConfigUtils.getOptionalBooleanValue(props, "bridgeAspectjMessagesToSLF4J",
                                                                               DEFAULT_BRIDGE_ASPECTJ_LOGGING_TO_SLF4J))
                          .codeBase(ConfigUtils.getMandatoryStringValue(props, "codeBase"))
                          .environment(ConfigUtils.getOptionalStringValue(props, "environment", DEFAULT_ENVIRONMENT))
                          .excludePackages(ConfigUtils.getOptionalStringValue(props, "excludePackages", ""))
                          .httpConnectTimeoutSeconds(
                                           ConfigUtils.getOptionalIntValue(props, "httpConnectTimeoutSeconds",
                                                                           DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS))
                          .httpProxyHost(ConfigUtils.getOptionalStringValue(props, "httpProxyHost", DEFAULT_HTTP_PROXY_HOST))
                          .httpProxyPort(ConfigUtils.getOptionalIntValue(props, "httpProxyPort", DEFAULT_HTTP_PROXY_PORT))
                          .httpReadTimeoutSeconds(
                                           ConfigUtils
                                               .getOptionalIntValue(props, "httpReadTimeoutSeconds", DEFAULT_HTTP_READ_TIMEOUT_SECONDS))
                          .httpWriteTimeoutSeconds(
                                           ConfigUtils
                                               .getOptionalIntValue(props, "httpWriteTimeoutSeconds", DEFAULT_HTTP_WRITE_TIMEOUT_SECONDS))
                          .licenseKey(ConfigUtils.getOptionalStringValue(props, "licenseKey", TRIAL_LICENSE_KEY))
                          .methodVisibility(
                                           ConfigUtils.getOptionalStringValue(props, "methodVisibility", DEFAULT_METHOD_VISIBILITY))
                          .packages(ConfigUtils.getMandatoryStringValue(props, "packages"))
                          .serverUrl(ConfigUtils.getOptionalStringValue(props, "serverUrl", DEFAULT_SERVER_URL))
                          .tags(ConfigUtils.getOptionalStringValue(props, TAGS_KEY, ""))
                          .build().validate();
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

    public static AgentConfig createSampleAgentConfig() {
        return AgentConfigFactory.createTemplateConfig().toBuilder()
                                 .appName("Sample Application Name")
                                 .codeBase(SAMPLE_CODEBASE_URI1 + " , " + SAMPLE_CODEBASE_URI2)
                                 .packages("com.acme. , foo.bar.")
                                 .excludePackages("some.excluded.package")
                                 .build();
    }

    public static AgentConfig createTemplateConfig() {
        return AgentConfig.builder()
                          .appName(UNSPECIFIED)
                          .appVersion(UNSPECIFIED)
                          .aspectFile(DEFAULT_ASPECT_FILE)
                          .aspectjOptions(SAMPLE_ASPECTJ_OPTIONS)
                          .bridgeAspectjMessagesToSLF4J(DEFAULT_BRIDGE_ASPECTJ_LOGGING_TO_SLF4J)
                          .codeBase(UNSPECIFIED)
                          .environment(DEFAULT_ENVIRONMENT)
                          .excludePackages("")
                          .httpConnectTimeoutSeconds(DEFAULT_HTTP_CONNECT_TIMEOUT_SECONDS)
                          .httpProxyHost(DEFAULT_HTTP_PROXY_HOST)
                          .httpProxyPort(DEFAULT_HTTP_PROXY_PORT)
                          .httpReadTimeoutSeconds(DEFAULT_HTTP_READ_TIMEOUT_SECONDS)
                          .httpWriteTimeoutSeconds(DEFAULT_HTTP_WRITE_TIMEOUT_SECONDS)
                          .licenseKey(TRIAL_LICENSE_KEY)
                          .methodVisibility(DEFAULT_METHOD_VISIBILITY)
                          .packages(UNSPECIFIED)
                          .serverUrl(DEFAULT_SERVER_URL)
                          .tags(createSystemPropertiesTags() + ", " + SAMPLE_TAGS)
                          .build();
    }

}
