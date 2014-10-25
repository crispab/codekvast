package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JvmRunTest {

    @Test
    public void testSaveAndRestore() throws IOException {
        File file = File.createTempFile("jvm-run", ".properties");
        file.deleteOnExit();
        JvmRun sr1 = JvmRun.builder()
                           .hostName("hostName")
                           .jvmFingerprint(UUID.randomUUID().toString())
                           .startedAtMillis(System.currentTimeMillis())
                           .codekvastVersion("codekvastVersion")
                           .codekvastVcsId("codekvastVcsId")
                           .build();
        sr1.saveTo(file);
        JvmRun sr2 = JvmRun.readFrom(file);
        assertEquals(sr1, sr2);
    }

}
