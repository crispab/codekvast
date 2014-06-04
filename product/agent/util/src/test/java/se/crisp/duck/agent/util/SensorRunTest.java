package se.crisp.duck.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SensorRunTest {

    @Test
    public void testSaveAndRestore() throws IOException {
        File file = File.createTempFile("sensorRun", ".properties");
        file.deleteOnExit();
        SensorRun sr1 = SensorRun.builder()
                                 .hostName("hostName")
                                 .uuid(UUID.randomUUID())
                                 .startedAtMillis(System.currentTimeMillis())
                                 .build();
        sr1.saveTo(file);
        SensorRun sr2 = SensorRun.readFrom(file);
        assertEquals(sr1, sr2);
    }

}
