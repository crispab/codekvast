package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AgentConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        AgentConfig config1 = AgentConfig.createSampleAgentConfig();
        File file = new File(System.getProperty("sampleAgentConfigFile.path", "build/codekvast-agent.conf.sample"));
        config1.saveTo(file);

        AgentConfig config2 = AgentConfig.parseAgentConfigFile(file.toURI());
        assertEquals(config1, config2);
    }

}
