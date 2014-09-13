package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AgentConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        AgentConfig config1 = AgentConfig.createSampleConfiguration();
        File file = new File(System.getProperty("sampleConfigFile.path", "build/codekvast.properties.sample"));
        config1.saveTo(file);

        AgentConfig config2 = AgentConfig.parseConfigFile(file.toURI());
        assertEquals(config1, config2);
    }

}
