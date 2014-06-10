package se.crisp.duck.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UsageDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void usageDataShouldBeJsonSerializable() throws IOException {
        // given
        Map<String, Long> usage = new HashMap<>();
        usage.put("sig1", 10000L);
        usage.put("sig2", 20000L);

        UsageData data1 = UsageData.builder().header(HeaderTest.HEADER).usage(usage).build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        UsageData data2 = objectMapper.readValue(json, UsageData.class);

        // then
        assertThat(data1, is(data2));
    }

}
