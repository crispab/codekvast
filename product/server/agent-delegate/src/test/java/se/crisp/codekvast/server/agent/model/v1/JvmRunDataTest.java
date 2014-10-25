package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JvmRunDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(expected = NullPointerException.class)
    public void jvmRunDataShouldRejectNullHeader() {
        JvmRunData.builder().header(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void jvmRunDataShouldRejectMissingValues() {
        JvmRunData.builder().header(HeaderTest.HEADER).build();
    }

    @Test
    public void jvmRunDataShouldBeJsonSerializable() throws IOException {
        // given
        JvmRunData data1 = JvmRunData.builder()
                                           .header(HeaderTest.HEADER)
                                           .hostName("hostName")
                                           .startedAtMillis(1000L)
                                           .dumpedAtMillis(2000L)
                                           .jvmFingerprint(UUID.randomUUID().toString())
                                           .codekvastVersion("codekvastVersion")
                                           .codekvastVcsId("codekvastVcsId")
                                           .build();
        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmRunData data2 = objectMapper.readValue(json, JvmRunData.class);

        // then
        assertThat(data1, is(data2));
    }

}
