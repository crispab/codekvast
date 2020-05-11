/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
package io.codekvast.common.util

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.math.log10
import kotlin.math.pow

/** @author olle.hallin@crisp.se
 */
object LoggingUtils {
    /**
     * Converts a number of bytes to a human readable string. Example: 12345 is converted to "12,3
     * kB". It uses the default locale for formatting the float.
     *
     * @param bytes The byte count
     * @return A human readable string rounded to one decimal.
     */
    @JvmStatic
    fun humanReadableByteCount(bytes: Long): String {
        if (bytes < 1000) {
            return "$bytes B"
        }
        val exponent = (log10(bytes.toDouble()) / log10(1000.0)).toInt()
        val unit = " kMGTPE"[exponent].toString() + "B"
        return String.format("%.1f %s", bytes / 1000.0.pow(exponent.toDouble()), unit)
    }

    @JvmStatic
    fun humanReadableDuration(duration: Duration): String {
        val roundToSeconds = duration.toMillis() > 2000
        val truncateTo = if (roundToSeconds) SECONDS else MILLIS
        val plusMillis = if (roundToSeconds) 500L else 0L
        return duration
                .plusMillis(plusMillis)
                .truncatedTo(truncateTo)
                .toString()
                .substring(2)
                .replace("(\\d[HMS])(?!$)".toRegex(), "$1 ")
                .toLowerCase()
    }

    @JvmStatic
    fun humanReadableDuration(first: Instant, last: Instant): String {
        return humanReadableDuration(Duration.between(first, last))
    }
}