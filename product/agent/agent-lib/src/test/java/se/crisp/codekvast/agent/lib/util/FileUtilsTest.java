package se.crisp.codekvast.agent.lib.util;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import se.crisp.codekvast.agent.lib.model.Invocation;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class FileUtilsTest {

    private static final String COMMENT = "some comment";
    private static final String SOME_URI = "file://go/figure";
    private static final String SOME_FILE_PATH = "/some/file/path";
    private static final int URI_LINE_NO = 3;
    private static final int FILE_LINE_NO = 2;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testResetConsumedInvocationsFiles() throws IOException {
        File invocationsFile = new File(temporaryFolder.getRoot(), "invocations.dat");

        File consumedInvocationsFile = new File(temporaryFolder.getRoot(), "invocations.dat" + FileUtils.CONSUMED_SUFFIX);
        writeTo(consumedInvocationsFile, "Hello, world!");

        File consumedInvocationsFile1 = new File(temporaryFolder.getRoot(), "invocations.dat.0001" + FileUtils.CONSUMED_SUFFIX);
        writeTo(consumedInvocationsFile1, "Hello, world!");

        FileUtils.resetAllConsumedInvocationDataFiles(invocationsFile);

        File[] files = temporaryFolder.getRoot().listFiles();
        assertThat(files, notNullValue());
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is("invocations.dat"));
        assertThat(files[1].getName(), is("invocations.dat.0001"));
    }

    @Test
    public void testProduceAndConsumeInvocationsFiles() throws IOException {
        File invocationsFile = new File(temporaryFolder.getRoot(), "invocations.dat");
        File consumedInvocationsFile = new File(temporaryFolder.getRoot(), "invocations.dat.00001" + FileUtils.CONSUMED_SUFFIX);
        writeTo(consumedInvocationsFile, "Hello, world!");
        File otherFile = new File(temporaryFolder.getRoot(), "zzz_other.file");
        writeTo(otherFile, "Hello, world!");

        long t1 = System.currentTimeMillis() - 60000;
        long t2 = System.currentTimeMillis() - 30000;
        FileUtils.writeInvocationDataTo(invocationsFile, 1, t1, setOf("public sig1.1()", "public sig1.2()", "public sig1.3()"));
        FileUtils.writeInvocationDataTo(invocationsFile, 2, t2, setOf("public sig2.1()", "public sig2.2()"));

        File[] files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(4));

        Arrays.sort(files);
        assertThat(files[0].getName(), is("invocations.dat.00000"));
        assertThat(files[1].getName(), CoreMatchers.is("invocations.dat.00001" + FileUtils.CONSUMED_SUFFIX));
        assertThat(files[2].getName(), is("invocations.dat.00002"));
        assertThat(files[3].getName(), is("zzz_other.file"));

        List<Invocation> invocations = FileUtils.consumeAllInvocationDataFiles(invocationsFile);
        assertThat(invocations.size(), is(5));

        files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(4));

        Arrays.sort(files);
        assertThat(files[0].getName(), CoreMatchers.is("invocations.dat.00000" + FileUtils.CONSUMED_SUFFIX));
        assertThat(files[1].getName(), CoreMatchers.is("invocations.dat.00001" + FileUtils.CONSUMED_SUFFIX));
        assertThat(files[2].getName(), CoreMatchers.is("invocations.dat.00002" + FileUtils.CONSUMED_SUFFIX));
        assertThat(files[3].getName(), is("zzz_other.file"));

        FileUtils.deleteAllConsumedInvocationDataFiles(invocationsFile);
        files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(1));
        assertThat(files[0].getName(), is("zzz_other.file"));
    }

    private void writeTo(File file, String message) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(message);
        writer.newLine();
        writer.close();
    }

    private Set<String> setOf(String... signatures) {
        Set<String> result = new HashSet<String>();
        Collections.addAll(result, signatures);
        return result;
    }

    @Test
    public void testReadPropertiesFromClasspath() throws URISyntaxException, IOException {
        URI codekvast1 = new URI("classpath:/codekvast1.conf");
        Properties properties = FileUtils.readPropertiesFrom(codekvast1);
        assertThat(properties.getProperty("appName"), is("appName"));
    }

    @Test
    public void testThatColonCharactersAreProtectedWithBackslash() throws IOException, URISyntaxException {
        File someFile = temporaryFolder.newFile();

        SomeTestClass someObject = new SomeTestClass();

        FileUtils.writePropertiesTo(someFile, someObject, COMMENT);

        String uriLine = getLineNo(someFile, URI_LINE_NO);
        assertThat(uriLine, equalTo("someURI = " + SOME_URI.replace(":", "\\:")));
    }

    @Test
    public void testThatBackSlashCharactersAreProtectedWithBackslash() throws IOException, URISyntaxException {
        File someFile = temporaryFolder.newFile();

        SomeTestClass someObject = new SomeTestClass();

        FileUtils.writePropertiesTo(someFile, someObject, COMMENT);

        String fileLine = getLineNo(someFile, FILE_LINE_NO);
        assertThat(fileLine, equalTo("someFile = " + someObject.someFile.toString().replace("\\", "\\\\")));
    }

    private String getLineNo(File someFile, int lineNo) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(someFile));
        for (int n = 0; n < lineNo; n++) {
            reader.readLine();
        }
        return reader.readLine();
    }

    private class SomeTestClass {
        File someFile;
        URI someURI;

        private SomeTestClass() throws URISyntaxException {
            someURI = new URI(SOME_URI);
            someFile = new File(SOME_FILE_PATH);
        }
    }
}
