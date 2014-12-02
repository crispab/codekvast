package se.crisp.codekvast.agent.main.support;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ManifestAppVersionStrategyTest {

    private static final String VALID_JAR_1_7_7 = "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar";

    private final AppVersionStrategy strategy = new ManifestAppVersionStrategy();

    @Test
    public void testResolveWhenValidJarFile() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7};

        assertThat(strategy.resolveAppVersion(args), is("1.7.7"));
    }

    @Test
    public void testResolveWhenInvalidJarFile() throws Exception {
        String args[] = {"manifest", VALID_JAR_1_7_7 + "XXX"};

        assertThat(strategy.resolveAppVersion(args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void testResolveWhenValidJarFile_butInvalidAttribute() throws Exception {
        String args[] = {"manifest", "src/test/resources/sample-app/lib/slf4j-api-1.7.7.jar", "foobar"};

        assertThat(strategy.resolveAppVersion(args), is(AppVersionStrategy.UNKNOWN_VERSION));
    }

    @Test
    public void testResolveWhenValidJarURI() throws Exception {
        String args[] = {"manifest", "file:" + System.getProperty("user.dir") + File.separator + VALID_JAR_1_7_7};

        assertThat(strategy.resolveAppVersion(args), is("1.7.7"));
    }

}
