package se.crisp.duck.agent.util;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class UsageTest {
    private final Usage usage = new Usage("signature", 1000L);

    @Test
    public void testToString() {
        assertThat(usage.toString(), is("          1000:signature"));
    }

    @Test
    public void testParseToString() throws Exception {
        assertThat(Usage.parse(usage.toString()), is(usage));
    }

    @Test
    public void testParseToStringTrimmed() throws Exception {
        assertThat(Usage.parse(usage.toString().trim()), is(usage));
    }

    @Test
    public void testParseNonMatchingLine() throws Exception {
        assertThat(Usage.parse("foobar"), nullValue());
    }

    @Test
    public void testParseBlankLine() throws Exception {
        assertThat(Usage.parse("  "), nullValue());
    }

    @Test
    public void testParseNullLine() throws Exception {
        assertThat(Usage.parse(null), nullValue());
    }

    @Test
    public void testReadNullFile() throws Exception {
        List<Usage> result = SensorUtils.readUsageFrom(null);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testReadFileUsage1() throws Exception {
        File file = new File(getClass().getResource("/usage1.dat").toURI());

        List<Usage> result = SensorUtils.readUsageFrom(file);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getUsedAtMillis(), is(1000L));
        assertThat(result.get(1).getUsedAtMillis(), is(0L));
    }
}
