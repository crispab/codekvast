package se.crisp.codekvast.server.agent_api.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JvmDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void jvmDataWithTagsShouldBeJsonSerializable() throws IOException {
        // given
        JvmData data1 = getJvmData("tag1, tag2, tag3");

        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmData data2 = objectMapper.readValue(json, JvmData.class);

        // then
        assertThat(data1, is(data2));
    }

    @Test
    public void jvmDataWithoutTagsShouldBeJsonSerializable() throws IOException {
        // given
        JvmData data1 = getJvmData("");

        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmData data2 = objectMapper.readValue(json, JvmData.class);

        // then
        assertThat(data1, is(data2));
    }

    private JvmData getJvmData(String tags) {
        return JvmData.builder()
                      .customerName("customerName")
                      .appName("appName")
                      .appVersion("appVersion")
                      .tags(tags)
                      .hostName("hostName")
                      .startedAtMillis(1000L)
                      .dumpedAtMillis(2000L)
                      .jvmFingerprint(UUID.randomUUID().toString())
                      .codekvastVersion("codekvastVersion")
                      .codekvastVcsId("codekvastVcsId")
                      .build();
    }

}
