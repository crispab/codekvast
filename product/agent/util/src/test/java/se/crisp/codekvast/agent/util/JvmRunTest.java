package se.crisp.codekvast.agent.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JvmRunTest {

    @Test
    public void testSaveAndRestore() throws IOException, URISyntaxException {
        File file = File.createTempFile("jvm-run", ".properties");
        file.deleteOnExit();
        JvmRun sr1 = JvmRun.builder()
                           .sharedConfig(SharedConfig.buildSampleSharedConfig())
                           .hostName("hostName")
                           .appName("appName")
                           .appVersion("appVersion")
                           .codeBaseUri(new URI("file:/foobar"))
                           .jvmFingerprint(UUID.randomUUID().toString())
                           .startedAtMillis(System.currentTimeMillis())
                           .build();
        sr1.saveTo(file);
        JvmRun sr2 = JvmRun.readFrom(file);
        assertEquals(sr1, sr2);
    }

}
