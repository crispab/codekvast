package se.crisp.duck.server.agent.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import se.crisp.duck.server.agent.model.v1.SignatureData;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SignatureDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void SignatureDataShouldBeJsonSerializable() throws IOException {
        SignatureData data1 = SignatureData.builder()
                                           .customerName("customerName")
                                           .appName("appName")
                                           .environment("environment")
                                           .signatures(Arrays.asList("sig1", "sig2"))
                                           .build();
        String json = objectMapper.writeValueAsString(data1);
        SignatureData data2 = objectMapper.readValue(json, SignatureData.class);

        assertThat(data1, is(data2));
    }
}
