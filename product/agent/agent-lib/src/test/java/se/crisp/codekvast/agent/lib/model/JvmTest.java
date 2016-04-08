package se.crisp.codekvast.agent.lib.model;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class JvmTest {

    @Test
    public void should_save_and_restore_to_file() throws IOException, URISyntaxException {
        File file = File.createTempFile("jvm-run", ".properties");
        file.deleteOnExit();
        Jvm jvm1 = Jvm.createSampleJvm();
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
