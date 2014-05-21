package duck.spike.util;

import org.junit.Test;

import java.io.File;
import java.util.Map;

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
        Map<String, Usage> result = UsageUtils.readFromFile(null);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testReadFileUsage1() throws Exception {
        File file = new File(getClass().getResource("/usage1.dat").toURI());

        Map<String, Usage> result = UsageUtils.readFromFile(file);

        assertThat(result.size(), is(2));
        assertThat(result.get("signature1").getUsedAtMillis(), is(1000L));
        assertThat(result.get("signature2").getUsedAtMillis(), is(0L));
        assertThat(result.get("signature3"), nullValue());
    }
}
