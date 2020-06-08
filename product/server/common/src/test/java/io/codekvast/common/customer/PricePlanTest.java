package io.codekvast.common.customer;

import static io.codekvast.common.customer.PricePlanDefaults.DEMO;
import static io.codekvast.common.customer.PricePlanDefaults.TEST;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

/** @author olle.hallin@crisp.se */
public class PricePlanTest {

  private static final Clock CLOCK = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private static final Instant NOW = CLOCK.instant();

  @Test
  public void should_create_from_DEMO_defaults() {
    assertThat(PricePlan.of(DEMO), not(nullValue()));
  }

  @Test
  public void should_create_from_TEST_defaults() {
    assertThat(PricePlan.of(TEST), not(nullValue()));
  }

  @Test
  public void shouldHandleNullParameters() {
    // given
    PricePlan pp = PricePlan.of(TEST);

    // when, then
    assertThat(pp.adjustTimestampMillis(null, CLOCK), is(nullValue()));
    assertThat(pp.adjustInstant(null, CLOCK), is(nullValue()));
    assertThat(pp.adjustCollectedDays(null), is(nullValue()));
  }

  @Test
  public void shouldIgnoreNegativeRetentionPeriod() {
    // given
    PricePlan pp = PricePlan.of(TEST).toBuilder().retentionPeriodDays(-1).build();

    Instant instant = NOW.minus(60, DAYS);

    // when, then
    assertThat(pp.adjustCollectedDays(65), is(65));
    assertThat(pp.adjustInstant(instant, CLOCK), is(instant));
    assertThat(pp.adjustInstantToMillis(instant, CLOCK), is(instant.toEpochMilli()));
    assertThat(pp.adjustTimestampMillis(instant.toEpochMilli(), CLOCK), is(instant.toEpochMilli()));
  }

  @Test
  public void shouldHandleInstantBeforeRetentionPeriod() {
    // given
    PricePlan pp = PricePlan.of(TEST).toBuilder().retentionPeriodDays(10).build();
    Instant instant = NOW.minus(60, DAYS);
    Instant expected = NOW.minus(10, DAYS);

    // when, then
    assertThat(pp.adjustCollectedDays(60), is(10));
    assertThat(pp.adjustInstant(instant, CLOCK), is(expected));
    assertThat(pp.adjustInstantToMillis(instant, CLOCK), is(expected.toEpochMilli()));
    assertThat(
        pp.adjustTimestampMillis(instant.toEpochMilli(), CLOCK), is(expected.toEpochMilli()));
  }

  @Test
  public void shouldHandleInstantWithinRetentionPeriod() {
    // given
    PricePlan pp = PricePlan.of(TEST).toBuilder().retentionPeriodDays(10).build();
    Instant instant = NOW.minus(5, DAYS);

    // when, then
    assertThat(pp.adjustCollectedDays(5), is(5));
    assertThat(pp.adjustInstant(instant, CLOCK), is(instant));
    assertThat(pp.adjustInstantToMillis(instant, CLOCK), is(instant.toEpochMilli()));
    assertThat(pp.adjustTimestampMillis(instant.toEpochMilli(), CLOCK), is(instant.toEpochMilli()));
  }
}
