package se.crisp.codekvast.server.codekvast_server.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DateUtilsTest {

    private static final long now = System.currentTimeMillis();

    @Test
    public void testGetAgeInMinutes() throws Exception {
        assertThat(DateUtils.getAge(now, now - 6 * 60 * 1000L - 10), is("6 min"));
    }

    @Test
    public void testGetAgeInHours() throws Exception {
        assertThat(DateUtils.getAge(now, now - 6 * 60 * 60 * 1000L - 10), is("6 hours"));
    }

    @Test
    public void testGetAgeInDays() throws Exception {
        assertThat(DateUtils.getAge(now, now - 6 * 24 * 60 * 60 * 1000L - 10), is("6 days"));
    }

    @Test
    public void testGetAgeInWeeks() throws Exception {
        assertThat(DateUtils.getAge(now, now - 40 * 24 * 60 * 60 * 1000L - 10), is("5 weeks"));
    }

}