package io.codekvast.javaagent.appversion;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ManifestAppVersionStrategyTest {

    private static final String VALID_JAR_1_7_7 = "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar";
    private static final String EXPECTED_VERSION = "1.7.7";

    private final Collection<File> VALID_PATHS;
    private final Collection<File> INVALID_PATHS;

    private final AppVersionStrategy strategy = new ManifestAppVersionStrategy();

    public ManifestAppVersionStrategyTest() throws URISyntaxException {
        VALID_PATHS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resources"));
        INVALID_PATHS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resourcesXXX"));
    }

    @Test
    public void should_resolve_when_valid_jar_name() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_not_resolve_when_invalid_jar_name() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7 + "XXX"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void should_resolve_when_valid_jar_name_but_invalid_attribute() throws Exception {
        String args[] = {"manifest", "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar", "foobar"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_jar_URI() throws Exception {
        String args[] = {"manifest", new File(System.getProperty("user.dir") + File.separator + VALID_JAR_1_7_7).toURI().toString()};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp() throws Exception {
        String args[] = {"manifest", "slf4j-api.*\\.jar$"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_naive_regexp() throws Exception {
        String args[] = {"manifest", "slf4j-api.*.jar"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp_and_invalid_attribute() throws Exception {
        String args[] = {"manifest", "slf4j-api.*", "foobar"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp_and_nonstandard_attribute() throws Exception {
        String args[] = {"manifest", "slf4j-api.*", "implementation-title"};

        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is("slf4j-api"));
    }

    @Test
    public void should_not_resolve_when_invalid_codeBase_and_valid_regexp() throws Exception {
        String args[] = {"manifest", "slf4j-api.*"};

        assertThat(strategy.resolveAppVersion(INVALID_PATHS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }
}
