package se.crisp.codekvast.server.codekvast_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import se.crisp.codekvast.server.codekvast_server.controller.RegistrationController.IsNameUniqueRequest;
import se.crisp.codekvast.server.codekvast_server.controller.RegistrationController.IsNameUniqueResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RegistrationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerializeIsUniqueRequest() throws Exception {
        // given
        IsNameUniqueRequest req1 = IsNameUniqueRequest.builder().kind("kind").name("name").build();

        // when
        String json = objectMapper.writeValueAsString(req1);
        IsNameUniqueRequest req2 =
                objectMapper.readValue(json, IsNameUniqueRequest.class);

        // then
        assertThat(req2, is(req1));
    }

    @Test
    public void testSerializeIsUniqueResponse() throws Exception {
        // given
        IsNameUniqueResponse rsp1 = IsNameUniqueResponse.builder().isUnique(true).build();

        // when
        String json = objectMapper.writeValueAsString(rsp1);
        IsNameUniqueResponse rsp2 =
                objectMapper.readValue(json, IsNameUniqueResponse.class);

        // then
        assertThat(rsp2, is(rsp1));
    }
}
