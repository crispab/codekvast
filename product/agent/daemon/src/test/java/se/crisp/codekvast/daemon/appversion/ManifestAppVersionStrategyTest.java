package se.crisp.codekvast.daemon.appversion;

import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ManifestAppVersionStrategyTest {

    private static final String VALID_JAR_1_7_7 = "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar";
    private static final String EXPECTED_VERSION = "1.7.7";

    private final Collection<File> VALID_URIS;
    private final Collection<File> INVALID_URIS;

    private final AppVersionStrategy strategy = new ManifestAppVersionStrategy();

    public ManifestAppVersionStrategyTest() throws URISyntaxException {
        VALID_URIS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resources"));
        INVALID_URIS = Arrays.asList(new File(System.getProperty("user.dir") + File.separator + "src/test/resourcesXXX"));
    }

    @Test
    public void testResolveWhen_validJarFile() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void testResolveWhen_invalidJarFile() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7 + "XXX"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void testResolveWhen_validJarFile_butInvalidAttribute() throws Exception {
        String args[] = {"manifest", "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar", "foobar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void testResolveWhen_validJarURI() throws Exception {
        String args[] = {"manifest", new File(System.getProperty("user.dir") + File.separator + VALID_JAR_1_7_7).toURI().toString()};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void testResolveWhen_validCodeBase_and_validRegexp() throws Exception {
        String args[] = {"manifest", "slf4j-api.*"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void testResolveWhen_validCodeBase_and_validRegexp_and_invalidAttribute() throws Exception {
        String args[] = {"manifest", "slf4j-api.*", "foobar"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is(EXPECTED_VERSION));
    }

    @Test
    public void testResolveWhen_validCodeBase_and_validRegexp_and_nonstandardAttribute() throws Exception {
        String args[] = {"manifest", "slf4j-api.*", "implementation-title"};

        assertThat(strategy.resolveAppVersion(VALID_URIS, args), is("slf4j-api"));
    }

    @Test
    public void testResolveWhen_invalidCodeBase_and_validRegexp() throws Exception {
        String args[] = {"manifest", "slf4j-api.*"};

        assertThat(strategy.resolveAppVersion(INVALID_URIS, args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }
}
