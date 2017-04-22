package se.crisp.codekvast.agent.lib.codebase;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CodeBaseFingerprintTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private final File files[] = new File[3];
    private final long now = 1492881351977L;

    @Before
    public void beforeTest() throws IOException {
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(folder.getRoot(), "/foo" + i);
            writeFile(files[i], "foo" + i);
        }
    }

    @Test
    public void should_be_equal_when_empty() throws IOException {
        // given
        CodeBaseFingerprint b1 = CodeBaseFingerprint.builder().build();
        CodeBaseFingerprint b2 = CodeBaseFingerprint.builder().build();

        // when

        // then
        assertThat(b1, equalTo(b2));
    }

    @Test
    public void should_not_be_equal_when_different_files() throws IOException {
        // given
        CodeBaseFingerprint.Builder b1 = CodeBaseFingerprint.builder();
        CodeBaseFingerprint.Builder b2 = CodeBaseFingerprint.builder();

        // when
        b1.record(files[1]);
        b2.record(files[2]);

        // then
        assertThat(b1.build(), not(equalTo(b2.build())));
    }

    @Test
    public void should_have_certain_value_when_empty() throws IOException {
        // given
        CodeBaseFingerprint fp1 = CodeBaseFingerprint.builder().build();

        // when

        // then
        assertThat(fp1.getValue(), is("r1Vw9aGBC3r3jK9LxwpmDw31HkK6+R1N5bIyjeDoPfw="));
    }

    @Test
    public void should_be_insensitive_to_file_order() throws IOException {
        // given
        CodeBaseFingerprint fp1 = CodeBaseFingerprint.builder().record(files[1]).record(files[2]).build();
        CodeBaseFingerprint fp2 = CodeBaseFingerprint.builder().record(files[2]).record(files[1]).build();

        // when

        // then
        assertThat(fp1, equalTo(fp2));
        assertThat(fp2, equalTo(fp1));
    }

    @Test
    public void should_include_last_modified_in_calculation() throws IOException {
        // given
        CodeBaseFingerprint fp1 = CodeBaseFingerprint.builder().record(files[1]).build();

        // when
        files[1].setLastModified(now + 10);
        CodeBaseFingerprint fp2 = CodeBaseFingerprint.builder().record(files[1]).build();

        // then
        assertThat(files[1].lastModified(), not(is(now)));
        assertThat(fp2, not(equalTo(fp1)));
    }

    @Test
    public void should_include_length_in_calculation() throws IOException {
        // given
        CodeBaseFingerprint fp1 = CodeBaseFingerprint.builder().record(files[1]).build();

        // when
        writeFile(files[1], "foobar");

        CodeBaseFingerprint fp2 = CodeBaseFingerprint.builder().record(files[1]).build();

        // then
        assertThat(fp2, not(equalTo(fp1)));
    }

    @SneakyThrows
    private void writeFile(File file, String contents) {
        PrintWriter os = new PrintWriter(new FileWriter(file));
        os.println(contents);
        os.flush();
        os.close();
    }

}
