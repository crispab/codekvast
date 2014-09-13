package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UsageDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void usageDataShouldBeJsonSerializable() throws IOException {
        // given

        UsageData data1 = UsageData.builder().header(HeaderTest.HEADER).usage(asList(
                new UsageDataEntry("sig1", 10000L, UsageDataEntry.CONFIDENCE_EXACT_MATCH),
                new UsageDataEntry("sig2", 20000L, UsageDataEntry.CONFIDENCE_FOUND_IN_PARENT_CLASS)))
                                   .build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        UsageData data2 = objectMapper.readValue(json, UsageData.class);

        // then
        assertThat(data1, is(data2));
    }

}
