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
    private static final String CLASSES_ONLY_DIR = "build/classes/java/main";

    private CodeBase codeBase;

    private CodeBase getCodeBase(String... codeBases) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        for (String s : codeBases) {
            sb.append(delimiter).append(new File(s).getAbsolutePath());
            delimiter = ", ";
        }

        return new CodeBase(AgentConfigFactory.createSampleAgentConfig()
                                              .toBuilder()
                                              .codeBase(sb.toString())
                                              .build());
    }

    @Test(expected = NullPointerException.class)
    public void should_handle_null_codeBase() throws Exception {
        codeBase = new CodeBase(null);
    }

    @Test
    public void should_handle_missing_codeBase() throws Exception {
        codeBase = getCodeBase("foobar");
        assertThat(codeBase.getUrls().length, is(0));
    }

    @Test
    public void should_handle_dir_containing_classes_but_no_jars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(CLASSES_ONLY_DIR);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

    @Test
    public void should_handle_directory_containing_only_jars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_LIB);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(2));
    }

    @Test
    public void should_handle_directories_containing_classes_and_jars() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(CLASSES_ONLY_DIR, SAMPLE_APP_LIB);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(3));
    }

    @Test
    public void should_handle_single_jar() throws MalformedURLException, URISyntaxException {
        codeBase = getCodeBase(SAMPLE_APP_JAR);
        assertThat(codeBase.getUrls(), notNullValue());
        assertThat(codeBase.getUrls().length, is(1));
    }

}
