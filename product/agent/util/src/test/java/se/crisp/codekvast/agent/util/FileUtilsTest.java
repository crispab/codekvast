package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileUtilsTest {

    public static final String COMMENT = "some comment";
    public static final String SOME_URI = "file://go/figure";
    public static final String SOME_FILE_PATH = "/some/file/path";
    public static final int URI_LINE_NO = 3;
    public static final int FILE_LINE_NO = 2;

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
