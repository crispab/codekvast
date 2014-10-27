package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CollectorConfigTest {

    @Test
    public void testSaveSampleConfigToFile() throws IOException {
        CollectorConfig config1 = CollectorConfig.createSampleCollectorConfig();
        File file = new File(System.getProperty("sampleCollectorConfigFile.path", "build/codekvast.conf.sample"));
        config1.saveTo(file);

        CollectorConfig config2 = CollectorConfig.parseCollectorConfigFile(file.toURI());
        assertEquals(config1, config2);
    }

}
