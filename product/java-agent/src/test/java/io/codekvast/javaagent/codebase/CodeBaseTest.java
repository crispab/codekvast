package io.codekvast.javaagent.codebase;

import io.codekvast.javaagent.config.AgentConfigFactory;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CodeBaseTest {

    private static final String SAMPLE_APP_LIB = "src/test/resources/sample-app/lib";
    private static final String SAMPLE_APP_JAR = SAMPLE_APP_LIB + "/sample-app.jar";

    private CodeBase codeBase;

    private CodeBase getCodeBase(String codeBase) {
        return new CodeBase(AgentConfigFactory.createSampleAgentConfig()
                                              .toBuilder()
                                              .codeBase(new File(codeBase).getAbsolutePath())
                                              .build());
    }

    @Test(expected = NullPointerException.class)
    public void testGetUrlsForNullConfig() throws Exception {
        codeBase = new CodeBase(null);
    }

    @Test
    public void testGetUrlsForMissingFile() throws Exception {
        codeBase = getCodeBase("foobar");
        assertThat(codeBase.getUrls().length, is(0));
    }

    @Test
    public void testGetUrlsForDirectoryWithoutJars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase("build/classes/main");
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

    @Test
    public void testGetUrlsForDirectoryContainingJars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_LIB);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));
    }

    @Test
    public void testGetUrlsForSingleJar() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

}
