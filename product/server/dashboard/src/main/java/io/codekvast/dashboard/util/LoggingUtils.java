/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.dashboard.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;

import static java.lang.String.format;

/**
 * @author olle.hallin@crisp.se
 */
@UtilityClass
public class LoggingUtils {

    /**
     * Converts a number of bytes to a human readable string. Example: 12345 is converted to "12,3 kB".
     * It uses the default locale for formatting the float.
     *
     * @param bytes The byte count
     * @return A human readable string rounded to one decimal.
     */
    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exponent = (int) (Math.log(bytes) / Math.log(1000));
        String unit = " kMGTPE".charAt(exponent) + "B";
        return format("%.1f %s", bytes / Math.pow(1000, exponent), unit);
    }

    public static String humanReadableDuration(Duration duration) {
        return duration.toString()
                       .substring(2)
                       .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                       .toLowerCase();
    }
}
