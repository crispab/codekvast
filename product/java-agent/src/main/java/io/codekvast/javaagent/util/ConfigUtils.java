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
package io.codekvast.javaagent.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for config stuff.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
@UtilityClass
public final class ConfigUtils {

    public static List<String> getNormalizedPackages(String packages) {
        List<String> result = new ArrayList<>();
        if (packages != null) {
            String[] prefixes = packages.split("[:;,]");
            for (String prefix : prefixes) {
                String trimmedPrefix = prefix.trim();
                if (!trimmedPrefix.isEmpty()) {
                    result.add(getNormalizedPackagePrefix(trimmedPrefix));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    static String getNormalizedPackagePrefix(String packagePrefix) {
        int dot = packagePrefix.length() - 1;
        while (dot >= 0 && packagePrefix.charAt(dot) == '.') {
            dot -= 1;
        }
        return packagePrefix.substring(0, dot + 1);
    }


    public static String getOptionalStringValue(Properties props, String propertyName, String defaultValue) {
        return expandVariables(props, propertyName, defaultValue);
    }

    static String expandVariables(Properties props, String key, String defaultValue) {
        String value = System.getenv(getEnvVarName(key));
        if (value == null) {
            value = props.getProperty(key, defaultValue);
        }
        return expandVariables(props, value);
    }

    static String expandVariables(Properties props, String value) {
        if (value == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\$(\\{([a-zA-Z0-9._-]+)}|([a-zA-Z0-9._-]+))");
        Matcher matcher = pattern.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key1 = matcher.group(2);
            String key2 = matcher.group(3);
            String key = key1 != null ? key1 : key2;
            String replacement = System.getProperty(key);
            if (replacement == null) {
                replacement = System.getenv(key);
            }
            if (replacement == null && props != null) {
                replacement = props.getProperty(key);
            }
            if (replacement == null) {
                String prefix = key1 != null ? "\\$\\{" : "\\$";
                String suffix = key1 != null ? "\\}" : "";
                replacement = String.format("%s%s%s", prefix, key, suffix);
                log.warn("Unrecognized variable: {}", replacement.replace("\\", ""));
            }

            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static String getEnvVarName(String propertyName) {
        return "CODEKVAST_" + propertyName.replaceAll("([A-Z])", "_$1").toUpperCase();
    }

    public static boolean getOptionalBooleanValue(Properties props, String key, boolean defaultValue) {
        return Boolean.valueOf(getOptionalStringValue(props, key, Boolean.toString(defaultValue)));
    }

    public static int getOptionalIntValue(Properties props, String key, int defaultValue) {
        return Integer.valueOf(getOptionalStringValue(props, key, Integer.toString(defaultValue)));
    }

    public static String getMandatoryStringValue(Properties props, String propertyName) {
        String value = expandVariables(props, propertyName, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing property: " + propertyName + " (or environment variable $" + getEnvVarName(propertyName) + ")");
        }
        return value;
    }

    public static List<File> getCommaSeparatedFileValues(String uriValues, boolean removeTrailingSlashes) {
        List<File> result = new ArrayList<>();
        String[] parts = uriValues.split("[;,]");
        for (String value : parts) {
            value = value.trim();
            if (removeTrailingSlashes && value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            result.add(new File(value));
        }
        return result;
    }

}
