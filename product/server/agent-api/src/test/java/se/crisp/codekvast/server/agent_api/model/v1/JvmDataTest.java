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
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .appName("appName")
                      .appVersion("appVersion")
                      .codekvastVcsId("codekvastVcsId")
                      .codekvastVersion("codekvastVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .dumpedAtMillis(2000L)
                      .jvmFingerprint(UUID.randomUUID().toString())
                      .startedAtMillis(1000L)
                      .tags(tags)
                      .build();
    }

}
