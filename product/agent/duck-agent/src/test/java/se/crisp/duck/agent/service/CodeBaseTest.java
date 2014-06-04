package se.crisp.duck.agent.service;

import org.junit.Test;

import java.net.MalformedURLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseTest {

    public static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    public static final String SAMPLE_APP_CLASSES = "../../sample/standalone-app/build/classes/main/";
    public static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private CodeBase codeBase;

    @Test(expected = NullPointerException.class)
    public void testGetUrlsForNullFile() throws Exception {
        codeBase = new CodeBase(null);
        codeBase.getUrls();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUrlsForMissingFile() throws Exception {
        codeBase = new CodeBase(" ");
        codeBase.getUrls();
    }

    @Test
    public void testGetUrlsForDirectoryWithoutJars() throws MalformedURLException {
        codeBase = new CodeBase("build/classes/main");
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

    @Test
    public void testGetUrlsForDirectoryContainingJars() throws MalformedURLException {
        codeBase = new CodeBase(SAMPLE_APP_LIB);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));
    }

    @Test
    public void testGetUrlsForSingleJar() throws MalformedURLException {
        codeBase = new CodeBase(SAMPLE_APP_JAR);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

}
