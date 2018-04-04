package io.codekvast.javaagent.appversion;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FilenameAppVersionStrategyTest {

    private final Collection<File> VALID_URIS;

    private final AppVersionStrategy strategy = new FilenameAppVersionStrategy();

    public FilenameAppVersionStrategyTest() {
        VALID_URIS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resources"));
    }

    @Test
    public void testResolveWhen_validGroupedPattern() {
        String args[] = {"filename", "slf4j-api-(.*).jar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("1.7.7"));
    }

    @Test
    public void testResolveWhen_validUngroupedPattern() {
        String args[] = {"filename", "slf4j-api-.*.jar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("slf4j-api-1.7.7.jar"));
    }

    @Test
    public void testResolveWhen_invalidPattern() {
        String args[] = {"filename", "foobar.jar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void testResolveWhen_illegalPattern() {
        String args[] = {"filename", "foo(bar.jar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }
}
