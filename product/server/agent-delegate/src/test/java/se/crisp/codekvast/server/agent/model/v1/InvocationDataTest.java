package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InvocationDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void invocationDataShouldBeJsonSerializable() throws IOException {
        // given

        InvocationData data1 = InvocationData.builder().header(HeaderTest.HEADER).invocations(asList(
                new InvocationEntry("sig1", 10000L, SignatureConfidence.EXACT_MATCH),
                new InvocationEntry("sig2", 20000L, SignatureConfidence.FOUND_IN_PARENT_CLASS)))
                                   .build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        InvocationData data2 = objectMapper.readValue(json, InvocationData.class);

        // then
        assertThat(data1, is(data2));
    }

}
