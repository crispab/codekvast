/*
 * Copyright (c) 2015-2022 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.javaagent.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

/**
 * Utility class for config stuff.
 *
 * @author olle.hallin@crisp.se
 */
@UtilityClass
@Log
public class ConfigUtils {

  public List<String> getNormalizedPackages(String packages) {
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

  String getNormalizedPackagePrefix(String packagePrefix) {
    int dot = packagePrefix.length() - 1;
    while (dot >= 0 && packagePrefix.charAt(dot) == '.') {
      dot -= 1;
    }
    return packagePrefix.substring(0, dot + 1);
  }

  public String getStringValue(Properties props, String propertyName, String defaultValue) {
    return expandVariables(props, propertyName, null).orElse(defaultValue);
  }

  public String getStringValue2(
      Properties props, String propertyName1, String propertyName2, String defaultValue) {
    return expandVariables(props, propertyName1, null)
        .orElseGet(() -> expandVariables(props, propertyName2, null).orElse(defaultValue));
  }

  public Optional<String> getStringValue(Properties props, String propertyName) {
    return expandVariables(props, propertyName, null);
  }

  public boolean getBooleanValue(Properties props, String key, boolean defaultValue) {
    return Boolean.parseBoolean(getStringValue(props, key, Boolean.toString(defaultValue)));
  }

  public int getIntValue(Properties props, String key, int defaultValue) {
    return Integer.parseInt(getStringValue(props, key, Integer.toString(defaultValue)));
  }

  Optional<String> expandVariables(Properties props, String key, String defaultValue) {
    String value = System.getProperty(getSystemPropertyName(key));
    if (value == null) {
      value = System.getenv(getEnvVarName(key));
    }
    if (value == null) {
      value = props.getProperty(key, defaultValue);
    }
    return value == null || value.trim().isEmpty()
        ? Optional.empty()
        : Optional.of(expandVariables(props, value));
  }

  String expandVariables(Properties props, String value) {
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
        logger.warning("Unrecognized variable: " + replacement.replace("\\", ""));
      }

      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  public String getEnvVarName(String propertyName) {
    return "CODEKVAST_" + propertyName.replaceAll("([A-Z])", "_$1").toUpperCase();
  }

  public String getSystemPropertyName(String key) {
    return "codekvast." + key;
  }

  public List<File> getCommaSeparatedFileValues(String uriValues) {
    List<File> result = new ArrayList<>();
    String[] parts = uriValues.split("[;,]");
    for (String value : parts) {
      result.add(new File(value.trim()));
    }
    return result;
  }
}
