package se.crisp.codekvast.agent.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CollectorConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        CollectorConfig config1 = CollectorConfig.createSampleCollectorConfig();
        File file = new File(System.getProperty("sampleCollectorConfigFile.path", "build/codekvast-collector.conf.sample"));
        config1.saveTo(file);

        CollectorConfig config2 = CollectorConfig.parseCollectorConfig(file.toURI(), null);
        assertEquals(config1, config2);
    }

    @Test
    public void testParseConfigFileURIWithOverride() throws IOException, URISyntaxException {
        CollectorConfig config1 = CollectorConfig.createSampleCollectorConfig();
        File file = new File(System.getProperty("sampleCollectorConfigFile.path", "build/codekvast.conf.sample"));
        config1.saveTo(file);

        CollectorConfig config2 = CollectorConfig.parseCollectorConfig(file.toURI(), "verbose=true");
        assertNotEquals(config1, config2);
        assertThat(config1.isVerbose(), is(false));
        assertThat(config2.isVerbose(), is(true));
    }

    @Test
    public void testParseConfigFilePathWithOverride() throws IOException, URISyntaxException {
        CollectorConfig config = CollectorConfig.parseCollectorConfig(new URI("classpath:/incomplete-collector-config.conf"),
                                                                      "appName=kaka;appVersion=version;");
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
    }

}
