package se.crisp.codekvast.agent.lib.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CollectorConfigTest {

    private CollectorConfig config1;
    private File file1;

    @Before
    public void beforeTest() throws Exception {
        config1 = CollectorConfigFactory.createSampleCollectorConfig();
        file1 = File.createTempFile("codekvast", ".conf");
        file1.deleteOnExit();
        CollectorConfigFactory.saveTo(config1, file1);
    }

    @After
    public void afterTest() throws Exception {
        System.clearProperty(CollectorConfigLocator.SYSPROP_OPTS);
    }

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        CollectorConfig config2 = CollectorConfigFactory.parseCollectorConfig(file1.toURI(), null);
        assertEquals(config1, config2);
    }

    @Test
    public void testParseConfigFileWithOverride() throws IOException, URISyntaxException {
        CollectorConfig config2 = CollectorConfigFactory.parseCollectorConfig(file1.toURI(), "verbose=true");
        assertNotEquals(config1, config2);
        assertThat(config1.isVerbose(), is(false));
        assertThat(config2.isVerbose(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseConfigFileWithIllegalAppNameOverride() throws IOException, URISyntaxException {
        CollectorConfigFactory.parseCollectorConfig(file1.toURI(), "appName=.illegalAppName");
    }

    @Test
    public void testParseConfigFilePathWithSyspropAndCmdLineOverride() throws IOException, URISyntaxException {
        System.setProperty(CollectorConfigLocator.SYSPROP_OPTS, "verbose=true;clobberAopXml=false;codeBase=/path/to/$appName");
        CollectorConfig config = CollectorConfigFactory.parseCollectorConfig(new URI("classpath:/incomplete-collector-config.conf"),
                                                                      "appName=kaka;appVersion=version;");
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
        assertThat(config.isVerbose(), is(true));
        assertThat(config.isClobberAopXml(), is(false));
        assertThat(config.getCodeBase(), is("/path/to/kaka"));
    }

    @Test
    public void testIsSyspropVerbose() {
        assertThat(CollectorConfigFactory.isSyspropVerbose(), is(false));

        System.setProperty(CollectorConfigLocator.SYSPROP_OPTS, "verbose=true;clobberAopXml=false;codeBase=/path/to/$appName");
        assertThat(CollectorConfigFactory.isSyspropVerbose(), is(true));

        System.setProperty(CollectorConfigLocator.SYSPROP_OPTS, "verbose=false;clobberAopXml=false;codeBase=/path/to/$appName");
        assertThat(CollectorConfigFactory.isSyspropVerbose(), is(false));
    }

}
