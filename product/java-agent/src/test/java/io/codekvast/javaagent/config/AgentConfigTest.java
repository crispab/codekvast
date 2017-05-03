package io.codekvast.javaagent.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AgentConfigTest {

    private AgentConfig config1;
    private File file1;

    @Before
    public void beforeTest() throws Exception {
        config1 = AgentConfigFactory.createSampleAgentConfig().toBuilder().appName("appName1").build();
        file1 = File.createTempFile("codekvast", ".conf");
        file1.deleteOnExit();
        AgentConfigFactory.saveTo(config1, file1);
    }

    @After
    public void afterTest() throws Exception {
        System.clearProperty(AgentConfigLocator.SYSPROP_OPTS);
    }

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        AgentConfig config2 = AgentConfigFactory.parseAgentConfig(file1.toURI(), null);
        assertEquals(config1, config2);
    }

    @Test
    public void testParseConfigFileWithOverride() throws IOException, URISyntaxException {
        AgentConfig config2 = AgentConfigFactory.parseAgentConfig(file1.toURI(), "appName=appName2");
        assertNotEquals(config1, config2);
        assertThat(config1.getAppName(), is("appName1"));
        assertThat(config2.getAppName(), is("appName2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseConfigFileWithIllegalAppNameOverride() throws IOException, URISyntaxException {
        AgentConfigFactory.parseAgentConfig(file1.toURI(), "appName=.illegalAppName");
    }

    @Test
    public void testParseConfigFilePathWithSyspropAndCmdLineOverride() throws IOException, URISyntaxException {
        System.setProperty(AgentConfigLocator.SYSPROP_OPTS, "clobberAopXml=false;codeBase=/path/to/$appName");
        AgentConfig config = AgentConfigFactory.parseAgentConfig(new URI("classpath:/incomplete-agent-config.conf"),
                                                                 "appName=kaka;appVersion=version;");
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
        assertThat(config.isClobberAopXml(), is(false));
        assertThat(config.getCodeBase(), is("/path/to/kaka"));
    }

    @Test
    public void testParsePre_0_16_0_ConfigFile() throws IOException, URISyntaxException {
        AgentConfig config = AgentConfigFactory.parseAgentConfig(new URI("classpath:/pre-0.16.0-config.conf"), null);
        assertThat(config.getPackages(), is("packages"));
        assertThat(config.getExcludePackages(), is("excludePackages"));
    }

    @Test
    public void testGetFilenamePrefix() throws Exception {
        AgentConfig config = AgentConfigFactory
            .createTemplateConfig()
            .toBuilder()
            .appName("   Some funky App name#'**  ")
            .appVersion("literal =)(%1.2./()¤%&3-Beta4+.RELEASE")
            .build();
        assertThat(config.getFilenamePrefix("prefix---"), is("prefix-somefunkyappname-1.2.3-beta4+.release-"));
    }
}
