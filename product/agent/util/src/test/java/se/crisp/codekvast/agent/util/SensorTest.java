package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SensorTest {

    @Test
    public void testSaveAndRestore() throws IOException {
        File file = File.createTempFile("sensorRun", ".properties");
        file.deleteOnExit();
        Sensor sr1 = Sensor.builder()
                           .hostName("hostName")
                           .uuid(UUID.randomUUID())
                           .startedAtMillis(System.currentTimeMillis())
                           .build();
        sr1.saveTo(file);
        Sensor sr2 = Sensor.readFrom(file);
        assertEquals(sr1, sr2);
    }

}
