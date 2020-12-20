package io.codekvast.common.util

import io.codekvast.common.util.LoggingUtils.humanReadableByteCount
import io.codekvast.common.util.LoggingUtils.humanReadableDuration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

/** @author olle.hallin@crisp.se
 */
class LoggingUtilsTest {
    private var oldLocale: Locale? = null

    @BeforeEach
    fun beforeTest() {
        oldLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterEach
    fun afterTest() {
        Locale.setDefault(oldLocale)
    }

    @ParameterizedTest
    @CsvSource(
            "123, 123 B",
            "12345, 12.3 kB",
            "123456789, 123.5 MB",
            "123456789012, 123.5 GB"
    )
    fun shouldMakeHumanReadableByteCount(bytes: Long, expected: String) {
        assertEquals(actual = humanReadableByteCount(bytes), expected = expected)
    }

    @ParameterizedTest
    @CsvSource(
            "123456000, 34h 17m 36s",
            "603000, 10m 3s",
            "12345, 12s",
            "1000, 1s",
            "1001, 1.001s",
            "1999, 1.999s",
            "2000, 2s",
            "2001, 2s",
            "2499, 2s",
            "2500, 3s",
            "2501, 3s")
    fun shouldMakeHumanReadableDuration(millis: Long, expected: String) {
        assertEquals(expected = expected, actual = humanReadableDuration(Duration.ofMillis(millis)))
    }
}