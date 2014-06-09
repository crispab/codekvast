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
        Map<String, Long> usage = new HashMap<>();
        usage.put("sig1", 10000L);
        usage.put("sig2", 20000L);

        UsageData data1 = UsageData.builder()
                                   .header(Header.builder()
                                                 .customerName("customerName")
                                                 .appName("appName")
                                                 .environment("environment")
                                                 .codeBaseName("codeBaseName").build())
                                   .usage(usage)
                                   .build();
        String json = objectMapper.writeValueAsString(data1);
        UsageData data2 = objectMapper.readValue(json, UsageData.class);

        assertThat(data1, is(data2));
    }

}
