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
        config1 = CollectorConfigFactory.createSampleCollectorConfig().toBuilder().appName("appName1").build();
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
        CollectorConfig config2 = CollectorConfigFactory.parseCollectorConfig(file1.toURI(), "appName=appName2");
        assertNotEquals(config1, config2);
        assertThat(config1.getAppName(), is("appName1"));
        assertThat(config2.getAppName(), is("appName2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseConfigFileWithIllegalAppNameOverride() throws IOException, URISyntaxException {
        CollectorConfigFactory.parseCollectorConfig(file1.toURI(), "appName=.illegalAppName");
    }

    @Test
    public void testParseConfigFilePathWithSyspropAndCmdLineOverride() throws IOException, URISyntaxException {
        System.setProperty(CollectorConfigLocator.SYSPROP_OPTS, "clobberAopXml=false;codeBase=/path/to/$appName");
        CollectorConfig config = CollectorConfigFactory.parseCollectorConfig(new URI("classpath:/incomplete-collector-config.conf"),
                                                                      "appName=kaka;appVersion=version;");
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
        assertThat(config.isClobberAopXml(), is(false));
        assertThat(config.getCodeBase(), is("/path/to/kaka"));
    }

    @Test
    public void testParsePre_0_16_0_ConfigFile() throws IOException, URISyntaxException {
        CollectorConfig config = CollectorConfigFactory.parseCollectorConfig(new URI("classpath:/pre-0.16.0-config.conf"), null);
        assertThat(config.getPackages(), is("packages"));
        assertThat(config.getExcludePackages(), is("excludePackages"));
    }

    @Test
    public void testCreateTemplateConfig() throws Exception {
        CollectorConfig config = CollectorConfigFactory.createTemplateConfig();
    }
}
