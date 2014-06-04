package se.crisp.duck.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ConfigurationTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        Configuration config1 = Configuration.getSampleConfiguration();
        File file = File.createTempFile("duck", ".properties");
        file.deleteOnExit();
        config1.saveTo(file);

        Configuration config2 = Configuration.parseConfigFile(file.getPath());
        assertEquals(config1, config2);

    }

}
