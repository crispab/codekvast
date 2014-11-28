package se.crisp.codekvast.agent.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CollectorConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        CollectorConfig config1 = CollectorConfig.createSampleCollectorConfig();
        File file = new File(System.getProperty("sampleCollectorConfigFile.path", "build/codekvast-collector.conf.sample"));
        config1.saveTo(file);

        CollectorConfig config2 = CollectorConfig.parseCollectorConfig(file.toURI());
        assertEquals(config1, config2);
    }

    @Test
    public void testParseConfigFileURIWithOverride() throws IOException, URISyntaxException {
        CollectorConfig config1 = CollectorConfig.createSampleCollectorConfig();
        File file = new File(System.getProperty("sampleCollectorConfigFile.path", "build/codekvast.conf.sample"));
        config1.saveTo(file);

        String args = file.toURI() + CollectorConfig.OVERRIDE_SEPARATOR + "verbose=true";
        CollectorConfig config2 = CollectorConfig.parseCollectorConfig(args);
        assertNotEquals(config1, config2);
        assertThat(config1.isVerbose(), is(false));
        assertThat(config2.isVerbose(), is(true));
    }

    @Test
    public void testParseConfigFilePathWithOverride() throws IOException, URISyntaxException {
        String args = "classpath:/incomplete-collector-config.conf;customerName=foobar;appName=kaka;appVersion=version;";
        CollectorConfig config = CollectorConfig.parseCollectorConfig(args);
        assertThat(config.getCustomerName(), is("foobar"));
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
    }

}
