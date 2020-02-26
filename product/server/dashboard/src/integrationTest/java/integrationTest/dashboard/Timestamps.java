package integrationTest.dashboard;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.sql.Timestamp;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/** @author olle.hallin@crisp.se */
@RequiredArgsConstructor
@Getter
class Timestamps {
  private final JdbcTemplate jdbcTemplate;

  private Timestamp almostThreeDaysAgo;
  private Timestamp tenMinutesAgo;
  private Timestamp twoMinutesAgo;
  private Timestamp inOneMinute;

  Timestamps invoke() {
    // Set the timestamps from Java. It's impossible to write time-zone agnostic code in a static
    // sql script invoked by @Sql.

    Instant now = Instant.now();
    almostThreeDaysAgo = Timestamp.from(now.minus(3, DAYS).minus(5, HOURS));
    tenMinutesAgo = Timestamp.from(now.minus(10, MINUTES));
    twoMinutesAgo = Timestamp.from(now.minus(2, MINUTES));
    inOneMinute = Timestamp.from(now.plus(1, MINUTES));

    jdbcTemplate.update(
        "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
        tenMinutesAgo,
        inOneMinute,
        TRUE,
        "uuid1");

    jdbcTemplate.update(
        "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
        tenMinutesAgo,
        inOneMinute,
        FALSE,
        "uuid2");

    jdbcTemplate.update(
        "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
        tenMinutesAgo,
        twoMinutesAgo,
        TRUE,
        "uuid3");

    jdbcTemplate.update(
        "UPDATE agent_state SET lastPolledAt = ?, nextPollExpectedAt = ?, enabled = ? WHERE jvmUuid = ? ",
        tenMinutesAgo,
        twoMinutesAgo,
        FALSE,
        "uuid4");

    jdbcTemplate.update(
        "UPDATE jvms SET startedAt = ?, publishedAt = ?", tenMinutesAgo, twoMinutesAgo);
    jdbcTemplate.update(
        "UPDATE jvms SET startedAt = ? WHERE uuid = ?", almostThreeDaysAgo, "uuid1");
    return this;
  }
}
