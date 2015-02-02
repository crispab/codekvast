package se.crisp.codekvast.server.agent_api.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SignatureDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void signatureDataShouldBeJsonSerializable() throws IOException {
        // given

        SignatureData data1 = SignatureData.builder()
                                           .jvmUuid("jvmUuid")
                                           .signatures(asList(
                                                   new SignatureEntry("sig1", 0L, null),
                                                   new SignatureEntry("sig2", 0L, null)))
                                             .build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        SignatureData data2 = objectMapper.readValue(json, SignatureData.class);

        // then
        assertThat(data1, is(data2));
    }

    @Test
    public void invocationDataShouldBeJsonSerializable() throws IOException {
        // given

        SignatureData data1 = SignatureData.builder()
                                           .jvmUuid("jvmUuid")
                                           .signatures(asList(
                                                   new SignatureEntry("sig1", 10000L, SignatureConfidence.EXACT_MATCH),
                                                   new SignatureEntry("sig2", 20000L, SignatureConfidence.FOUND_IN_PARENT_CLASS)))
                                             .build();

        // when
        String json = objectMapper.writeValueAsString(data1);
        SignatureData data2 = objectMapper.readValue(json, SignatureData.class);

        // then
        assertThat(data1, is(data2));
    }

}
