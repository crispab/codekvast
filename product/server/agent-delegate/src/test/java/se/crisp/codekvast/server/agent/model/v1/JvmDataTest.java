package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JvmDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(expected = NullPointerException.class)
    public void jvmDataShouldRejectNullHeader() {
        JvmData.builder().header(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void jvmDataShouldRejectMissingValues() {
        JvmData.builder().header(HeaderTest.HEADER).build();
    }

    @Test
    public void jvmDataShouldBeJsonSerializable() throws IOException {
        // given
        JvmData data1 = JvmData.builder()
                               .header(HeaderTest.HEADER)
                               .appName("appName")
                               .appVersion("appVersion")
                               .hostName("hostName")
                               .startedAtMillis(1000L)
                               .dumpedAtMillis(2000L)
                               .jvmFingerprint(UUID.randomUUID().toString())
                               .codekvastVersion("codekvastVersion")
                               .codekvastVcsId("codekvastVcsId")
                               .build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmData data2 = objectMapper.readValue(json, JvmData.class);

        // then
        assertThat(data1, is(data2));
    }

}
