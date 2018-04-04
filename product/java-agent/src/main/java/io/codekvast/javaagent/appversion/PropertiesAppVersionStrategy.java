/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.javaagent.appversion;

import lombok.extern.java.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

/**
 * A strategy for picking the app version from one or more properties in a properties file.
 * <p>
 * It handles the cases {@code properties somefile.conf prop1[,prop2...]}. The values of properties (separated by a dash '-') are used
 * as resolved version.
 *</p>
 * @author olle.hallin@crisp.se
 */
@Log
public class PropertiesAppVersionStrategy extends AbstractAppVersionStrategy {

    PropertiesAppVersionStrategy() {
        super("properties", "property");
    }

    @Override
    public String resolveAppVersion(Collection<File> codeBases, String[] args) {
        if (args == null || args.length < 3) {
            logger.severe(String.format("Cannot resolve '%s': missing args", join(args)));
            return UNKNOWN_VERSION;
        }

        File file = new File(args[1]);
        if (!file.canRead()) {
            logger.severe(String.format("Cannot resolve '%s': file not found: %s", join(args), file));
            return UNKNOWN_VERSION;
        }

        Properties props = new Properties();

        try(BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            props.load(is);
        } catch (IOException e) {
            logger.severe("Cannot load " + file + ": " + e.getMessage());
            return UNKNOWN_VERSION;
        }

        StringBuilder sb = new StringBuilder();
        String delimiter = "";

        for (int i = 2; i < args.length; i++) {
            String key = args[i];
            String value = props.getProperty(key);
            if (value == null) {
                logger.warning("Cannot find " + key + " in " + file.getAbsolutePath());
            } else {
                sb.append(delimiter).append(value);
                delimiter = "-";
            }
        }
        return sb.toString();
    }

    private String join(String args[]) {
        StringBuilder sb = new StringBuilder();
        String delimiter ="";
        for (String arg : args) {
            sb.append(delimiter).append(arg);
            delimiter = " ";
        }
        return sb.toString();
    }
}
