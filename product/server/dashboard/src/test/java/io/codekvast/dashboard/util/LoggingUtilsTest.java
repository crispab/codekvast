package io.codekvast.dashboard.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Locale;

import static io.codekvast.dashboard.util.LoggingUtils.humanReadableByteCount;
import static io.codekvast.dashboard.util.LoggingUtils.humanReadableDuration;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class LoggingUtilsTest {

    private Locale oldLocale;

    @Before
    public void beforeTest() {
        oldLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @After
    public void afterTest() {
        Locale.setDefault(oldLocale);
    }

    @Test
    public void shouldMakeHumanReadableByteCount() {
        assertThat(humanReadableByteCount(123456789L), is("123.5 MB"));
    }

    @Test
    public void shouldMakeHumanReadableDuration() {
        assertThat(humanReadableDuration(Duration.ofSeconds(123456L)), is("34h 17m 36s"));
    }
}
