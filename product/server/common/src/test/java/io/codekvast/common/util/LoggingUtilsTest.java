package io.codekvast.common.util;

import static io.codekvast.common.util.LoggingUtils.humanReadableByteCount;
import static io.codekvast.common.util.LoggingUtils.humanReadableDuration;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** @author olle.hallin@crisp.se */
public class LoggingUtilsTest {

  private Locale oldLocale;

  @BeforeEach
  public void beforeTest() {
    oldLocale = Locale.getDefault();
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterEach
  public void afterTest() {
    Locale.setDefault(oldLocale);
  }

  @Test
  public void shouldMakeHumanReadableByteCount() {
    assertThat(humanReadableByteCount(123456789L), is("123.5 MB"));
  }

  @ParameterizedTest
  @CsvSource({
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
    "2501, 3s"
  })
  public void shouldMakeHumanReadableDuration(long millis, String expected) {
    assertThat(humanReadableDuration(Duration.ofMillis(millis)), is(expected));
  }
}
