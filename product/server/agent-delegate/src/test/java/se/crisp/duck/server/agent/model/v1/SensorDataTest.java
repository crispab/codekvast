package se.crisp.duck.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SensorDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test(expected = NullPointerException.class)
    public void sensorDataShouldRejectNullHeader() {
        SensorData.builder().header(null).build();
    }

    @Test(expected = NullPointerException.class)
    public void sensorDataShouldRejectMissingValues() {
        SensorData.builder().header(HeaderTest.HEADER).build();
    }

    @Test
    public void sensorDataShouldBeJsonSerializable() throws IOException {
        // given
        SensorData data1 = SensorData.builder()
                                     .header(HeaderTest.HEADER)
                                     .hostName("hostName")
                                     .startedAtMillis(1000L)
                                     .dumpedAtMillis(2000L)
                                     .uuid(UUID.randomUUID())
                                     .build();
        // when
        String json = objectMapper.writeValueAsString(data1);
        SensorData data2 = objectMapper.readValue(json, SensorData.class);

        // then
        assertThat(data1, is(data2));
    }

}
