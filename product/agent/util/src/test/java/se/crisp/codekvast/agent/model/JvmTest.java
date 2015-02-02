package se.crisp.codekvast.agent.model;

import org.junit.Test;
import se.crisp.codekvast.agent.config.CollectorConfig;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JvmTest {

    @Test
    public void testSaveAndRestore() throws IOException, URISyntaxException {
        File file = File.createTempFile("jvm-run", ".properties");
        file.deleteOnExit();
        Jvm sr1 = Jvm.builder()
                     .collectorConfig(CollectorConfig.createSampleCollectorConfig())
                     .computerId("computerId")
                           .hostName("hostName")
                     .jvmUuid(UUID.randomUUID().toString())
                           .startedAtMillis(System.currentTimeMillis())
                           .build();
        sr1.saveTo(file);
        Jvm sr2 = Jvm.readFrom(file);
        assertEquals(sr1, sr2);
    }

}
