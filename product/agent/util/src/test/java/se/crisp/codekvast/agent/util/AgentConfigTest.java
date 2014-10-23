package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AgentConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        AgentConfig config1 = AgentConfig.createSampleConfiguration();
        File file = new File(System.getProperty("sampleConfigFile.path", "build/codekvast.properties.sample"));
        config1.saveTo(file);

        AgentConfig config2 = AgentConfig.parseConfigFile(file.toURI());
        assertEquals(config1, config2);
    }

    @Test
    public void testGetNormalizedPackagePrefix1() throws URISyntaxException {
        AgentConfig config = getAgentConfig("prefix.....");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix2() throws URISyntaxException {
        AgentConfig config = getAgentConfig("prefix.");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix3() throws URISyntaxException {
        AgentConfig config = getAgentConfig("prefix.foobar...");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix.foobar"));
    }

    @Test
    public void testGetNormalizedPackagePrefix4() throws URISyntaxException {
        AgentConfig config = getAgentConfig("prefix");
        assertThat(config.getNormalizedPackagePrefix(), is("prefix"));
    }

    @Test
    public void testGetNormalizedPackagePrefix5() throws URISyntaxException {
        AgentConfig config = getAgentConfig("p");
        assertThat(config.getNormalizedPackagePrefix(), is("p"));
    }

    @Test
    public void testGetNormalizedPackagePrefix6() throws URISyntaxException {
        AgentConfig config = getAgentConfig("");
        assertThat(config.getNormalizedPackagePrefix(), is(""));
    }

    private AgentConfig getAgentConfig(String packagePrefix) throws URISyntaxException {
        return AgentConfig.builder()
                          .customerName("customerName")
                          .appName("appName")
                          .appVersion("appVersion")
                          .environment("environment")
                          .codeBaseUri(new URI("file:/foobar"))
                          .aspectjOptions("aspectjOptions")
                          .dataPath(new File("."))
                          .serverUri(new URI("http://foobar"))
                          .apiUsername("username")
                          .apiPassword("password")
                          .packagePrefix(packagePrefix).build();
    }
}
