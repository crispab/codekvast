package io.codekvast.javaagent.config;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class AgentConfigTest {

    private File file = classpathResourceAsFile("/codekvast1.conf");
    private AgentConfig config = AgentConfigFactory.parseAgentConfig(file, null);

    @After
    public void afterTest() throws Exception {
        System.clearProperty(AgentConfigLocator.SYSPROP_OPTS);
    }

    @Test
    public void testParseConfigFileWithOverride() throws IOException, URISyntaxException {
        AgentConfig config2 = AgentConfigFactory.parseAgentConfig(file, "appName=appName2");
        assertThat(config, not(is(config2)));
        assertThat(config.getAppName(), is("appName1"));
        assertThat(config2.getAppName(), is("appName2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseConfigFileWithIllegalAppNameOverride() throws IOException, URISyntaxException {
        AgentConfigFactory.parseAgentConfig(file, "appName=.illegalAppName");
    }

    @Test
    public void testParseConfigFilePathWithSyspropAndCmdLineOverride() throws IOException, URISyntaxException {
        System.setProperty(AgentConfigLocator.SYSPROP_OPTS, "codeBase=/path/to/$appName");
        AgentConfig config = AgentConfigFactory.parseAgentConfig(
            classpathResourceAsFile("/incomplete-agent-config.conf"),
            "appName=kaka;appVersion=version;");
        assertThat(config.getAppName(), is("kaka"));
        assertThat(config.getAppVersion(), is("version"));
        assertThat(config.getCodeBase(), is("/path/to/kaka"));
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

    @SneakyThrows(URISyntaxException.class)
    private File classpathResourceAsFile(String resourceName) {
        return new File(getClass().getResource(resourceName).toURI());
    }

}
