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

    public ManifestAppVersionStrategyTest() {
        VALID_PATHS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resources"));
        INVALID_PATHS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resourcesXXX"));
    }

    @Test
    public void should_not_handle_unknown_strategy_name() {
        String args[] = {"foobar", "foobar.jar"};

        assertThat(strategy.canHandle(args), is(false));
    }

    @Test
    public void should_not_handle_when_missing_args() {
        String args[] = {"manifest"};

        assertThat(strategy.canHandle(args), is(false));
    }

    @Test
    public void should_handle_when_one_arg() {
        String args[] = {"manifest", "filename"};

        assertThat(strategy.canHandle(args), is(true));
    }

    @Test
    public void should_handle_when_two_args() {
        String args[] = {"manifest", "filename", "attribute"};

        assertThat(strategy.canHandle(args), is(true));
    }

    @Test
    public void should_not_handle_too_many_args() {
        String args[] = {"manifest", "filename", "attribute1", "attribute2"};

        assertThat(strategy.canHandle(args), is(false));
    }

    @Test
    public void should_resolve_when_valid_jar_name() {
        String args[] = {"manifest", VALID_JAR_1_7_7};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_not_resolve_when_invalid_jar_name() {
        String args[] = {"manifest", VALID_JAR_1_7_7 + "XXX"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void should_resolve_when_valid_jar_name_but_invalid_attribute() {
        String args[] = {"manifest", "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar", "foobar"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_jar_URI() {
        String args[] = {"manifest", new File(System.getProperty("user.dir") + File.separator + VALID_JAR_1_7_7).toURI().toString()};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp() {
        String args[] = {"manifest", "slf4j-api.*\\.jar$"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_naive_regexp() {
        String args[] = {"manifest", "slf4j-api.*.jar"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp_and_invalid_attribute() {
        String args[] = {"manifest", "slf4j-api.*", "foobar"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void should_resolve_when_valid_codeBase_and_valid_regexp_and_nonstandard_attribute() {
        String args[] = {"manifest", "slf4j-api.*", "implementation-title"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(VALID_PATHS, args), is("slf4j-api"));
    }

    @Test
    public void should_not_resolve_when_invalid_codeBase_and_valid_regexp() {
        String args[] = {"manifest", "slf4j-api.*"};

        assertThat(strategy.canHandle(args), is(true));
        assertThat(strategy.resolveAppVersion(INVALID_PATHS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }
}
