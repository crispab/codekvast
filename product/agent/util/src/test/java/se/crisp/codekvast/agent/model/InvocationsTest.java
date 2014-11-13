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

public class InvocationsTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testReadNullFile() throws Exception {
        List<Invocation> result = FileUtils.readInvocationDataFrom(null);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void testWriteReadInvocationsFile() throws Exception {
        // given
        File invocationsFile = new File(temporaryFolder.getRoot(), "invocations.dat");
        Set<String> signatures = new TreeSet<String>(Arrays.asList("sig2", "sig0", "sig1"));

        // when
        FileUtils.writeInvocationDataTo(invocationsFile, 1, 1000L, signatures, true);
        List<Invocation> result = FileUtils.readInvocationDataFrom(invocationsFile);

        // then
        assertThat(result.size(), is(3));
        for (int i = 0; i < signatures.size(); i++) {
            assertThat(result.get(i).getInvokedAtMillis(), is(1000L));
            assertThat(result.get(i).getSignature(), is("sig" + i));
        }
    }
}
