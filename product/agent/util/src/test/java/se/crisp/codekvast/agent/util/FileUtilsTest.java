package se.crisp.codekvast.agent.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileUtilsTest {

    public static final String COMMENT = "some comment";
    public static final String SOME_URI = "file://go/figure";
    public static final String SOME_FILE_PATH = "/some/file/path";
    public static final int URI_LINE_NO = 3;
    public static final int FILE_LINE_NO = 2;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testResetConsumedUsageFiles() throws IOException {
        File usageFile = new File(temporaryFolder.getRoot(), "usage.dat");

        File consumedUsageFile = new File(temporaryFolder.getRoot(), "usage.dat.consumed");
        writeTo(consumedUsageFile, "Hello, world!");

        File consumedUsageFile1 = new File(temporaryFolder.getRoot(), "usage.dat.1.consumed");
        writeTo(consumedUsageFile1, "Hello, world!");

        FileUtils.resetAllConsumedUsageDataFiles(usageFile);

        File[] files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(2));
        Arrays.sort(files);
        assertThat(files[0].getName(), is("usage.dat"));
        assertThat(files[1].getName(), is("usage.dat.1"));
    }

    @Test
    public void testProduceAndConsumeUsageFiles() throws IOException {
        File usageFile = new File(temporaryFolder.getRoot(), "usage.dat");
        File consumedUsageFile = new File(temporaryFolder.getRoot(), "usage.dat.1.consumed");
        writeTo(consumedUsageFile, "Hello, world!");
        File otherFile = new File(temporaryFolder.getRoot(), "zzz_other.file");
        writeTo(otherFile, "Hello, world!");

        long t1 = System.currentTimeMillis() - 60000;
        long t2 = System.currentTimeMillis() - 30000;
        FileUtils.writeUsageDataTo(usageFile, 1, t1, setOf("sig1.1", "sig1.2", "sig1.3"));
        FileUtils.writeUsageDataTo(usageFile, 2, t2, setOf("sig2.1", "sig2.2"));

        File[] files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(4));

        Arrays.sort(files);
        assertThat(files[0].getName(), is("usage.dat"));
        assertThat(files[1].getName(), is("usage.dat.1.consumed"));
        assertThat(files[2].getName(), is("usage.dat.2"));
        assertThat(files[3].getName(), is("zzz_other.file"));

        List<Usage> usages = FileUtils.consumeAllUsageDataFiles(usageFile);
        assertThat(usages.size(), is(5));

        files = temporaryFolder.getRoot().listFiles();
        assertThat(files.length, is(4));

        Arrays.sort(files);
        assertThat(files[0].getName(), is("usage.dat.1.consumed"));
        assertThat(files[1].getName(), is("usage.dat.2.consumed"));
        assertThat(files[2].getName(), is("usage.dat.consumed"));
        assertThat(files[3].getName(), is("zzz_other.file"));

        FileUtils.deleteAllConsumedUsageDataFiles(usageFile);
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
        for (String s : signatures) {
            result.add(s);
        }
        return result;
    }

    @Test
    public void testReadPropertiesFromClasspath() throws URISyntaxException, IOException {
        URI codekvast1 = new URI("classpath:/codekvast1.properties");
        Properties properties = FileUtils.readPropertiesFrom(codekvast1);
        assertThat(properties.getProperty("appName"), is("appName"));
    }

    @Test
    public void testThatColonCharactersAreProtectedWithBackslash() throws IOException, URISyntaxException {
        File someFile = File.createTempFile("codekvast-test", ".properties");
        SomeTestClass someObject = new SomeTestClass();

        FileUtils.writePropertiesTo(someFile, someObject, COMMENT);

        String uriLine = getLineNo(someFile, URI_LINE_NO);
        assertThat(uriLine, equalTo("someURI = " + SOME_URI.replace(":", "\\:")));
    }

    @Test
    public void testThatBackSlashCharactersAreProtectedWithBackslash() throws IOException, URISyntaxException {
        File someFile = File.createTempFile("codekvast-test", ".properties");
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
