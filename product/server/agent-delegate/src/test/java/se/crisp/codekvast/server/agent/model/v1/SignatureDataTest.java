package se.crisp.codekvast.server.agent.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SignatureDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void signatureDataShouldBeJsonSerializable() throws IOException {
        // given
        SignatureData data1 =
                SignatureData.builder().header(HeaderTest.HEADER).jvmFingerprint(UUID.randomUUID().toString()).signatures(Arrays.asList
                        ("sig1", "sig2")).build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        SignatureData data2 = objectMapper.readValue(json, SignatureData.class);

        // then
        assertThat(data1, is(data2));
    }
}
