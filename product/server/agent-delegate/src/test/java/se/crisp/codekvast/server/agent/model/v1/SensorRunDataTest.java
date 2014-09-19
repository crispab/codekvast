package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SensorRunDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(expected = NullPointerException.class)
    public void sensorRunDataShouldRejectNullHeader() {
        SensorRunData.builder().header(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void sensorRunDataShouldRejectMissingValues() {
        SensorRunData.builder().header(HeaderTest.HEADER).build();
    }

    @Test
    public void sensorRunDataShouldBeJsonSerializable() throws IOException {
        // given
        SensorRunData data1 = SensorRunData.builder()
                                           .header(HeaderTest.HEADER)
                                           .hostName("hostName")
                                           .startedAtMillis(1000L)
                                           .dumpedAtMillis(2000L)
                                           .uuid(UUID.randomUUID())
                                           .build();
        // when
        String json = objectMapper.writeValueAsString(data1);
        SensorRunData data2 = objectMapper.readValue(json, SensorRunData.class);

        // then
        assertThat(data1, is(data2));
    }

}
