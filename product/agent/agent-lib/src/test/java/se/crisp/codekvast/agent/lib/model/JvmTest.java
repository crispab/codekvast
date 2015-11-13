package se.crisp.codekvast.agent.lib.model;

import org.junit.Test;
import se.crisp.codekvast.agent.lib.config.CollectorConfigFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class JvmTest {

    @Test
    public void testSaveAndRestore() throws IOException, URISyntaxException {
        File file = File.createTempFile("jvm-run", ".properties");
        file.deleteOnExit();
        Jvm jvm1 = Jvm.builder()
                     .collectorConfig(CollectorConfigFactory.createSampleCollectorConfig())
                     .collectorVcsId("collectorVcsId")
                     .collectorVersion("collectorVersion")
                     .computerId("computerId")
                     .hostName("hostName")
                     .jvmUuid(UUID.randomUUID().toString())
                     .startedAtMillis(System.currentTimeMillis())
                     .build();
        jvm1.saveTo(file);
        Jvm jvm2 = Jvm.readFrom(file);
        assertEquals(jvm1, jvm2);

        String[] strings = jvm2.getCollectorConfig().getTags().split("\\s*,\\s*");
        assertThat(strings.length, is(7));
        assertThat(strings[0], startsWith("java.runtime.name="));
        assertThat(strings[5], is("key1=value1"));
        assertThat(strings[6], is("key2=value2"));
    }

}
