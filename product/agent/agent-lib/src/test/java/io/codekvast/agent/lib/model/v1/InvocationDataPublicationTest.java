package io.codekvast.agent.lib.model.v1;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class InvocationDataPublicationTest {

    @Test
    public void should_have_decent_toString() throws Exception {
        InvocationDataPublication p = InvocationDataPublication
            .builder()
            .appName("appName")
            .appVersion("appVersion")
            .codeBaseFingerprint("codeBaseFingerprint")
            .collectorVersion("collectorVersion")
            .computerId("computerId")
            .hostName("hostName")
            .jvmUuid("jvmUuid")
            .publicationCount(3)
            .invocations(Collections.<String>emptySet())
            .recordingIntervalStartedAtMillis(toMillis(2016, 1, 2, 10, 11, 12))
            .publishedAtMillis(toMillis(2017, 3, 4, 13, 14, 15))
            .build();

        assertThat(p.toString(), containsString("appName='appName'"));
        assertThat(p.toString(), containsString("appVersion='appVersion'"));
        assertThat(p.toString(), containsString("publication=#3"));
        assertThat(p.toString(), containsString("interval=[2016-01-02:10:11:12+0100--2017-03-04:13:14:15+0100]"));
    }

    private long toMillis(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        return c.getTime().getTime();
    }
}