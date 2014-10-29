package se.crisp.codekvast.agent.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UsageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testReadNullFile() throws Exception {
        List<Usage> result = FileUtils.readUsageDataFrom(null);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testWriteReadUsageFile() throws Exception {
        // given
        File usageFile = new File(temporaryFolder.getRoot(), "usage.dat");
        Set<String> signatures = new TreeSet<String>(Arrays.asList("sig2", "sig0", "sig1"));

        // when
        FileUtils.writeUsageDataTo(usageFile, 1, 1000L, signatures, true);
        List<Usage> result = FileUtils.readUsageDataFrom(usageFile);

        // then
        assertThat(result.size(), is(3));
        for (int i = 0; i < signatures.size(); i++) {
            assertThat(result.get(i).getUsedAtMillis(), is(1000L));
            assertThat(result.get(i).getSignature(), is("sig" + i));
        }
    }
}
