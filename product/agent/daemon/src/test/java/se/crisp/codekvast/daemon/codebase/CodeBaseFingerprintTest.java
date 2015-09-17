package se.crisp.codekvast.daemon.codebase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class CodeBaseFingerprintTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private final File files[] = new File[3];

    @Before
    public void beforeTest() throws IOException {
        for (int i = 0; i < files.length; i++) {
            files[i] = folder.newFile();
        }
    }

    @Test
    public void testEquals() throws IOException {
        CodeBaseFingerprint.Builder builder1 = CodeBaseFingerprint.builder();
        CodeBaseFingerprint.Builder builder2 = CodeBaseFingerprint.builder();
        assertThat(builder1.build(), equalTo(builder2.build()));

        builder1.record(files[0]).record(files[1]);
        builder2.record(files[0]).record(files[1]);
        assertThat(builder1.build(), equalTo(builder2.build()));

        builder2.record(files[2]);
        assertThat(builder1.build(), not(equalTo(builder2.build())));
    }

}
