package io.codekvast.javaagent.model.model.v1.rest;

import com.google.gson.Gson;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests that Gson plays well together with the Lombok-generated stuff.
 *
 * @author olle.hallin@crisp.se
 */
public class GetConfigRequest1Test {
    private final Gson gson = new Gson();

    @Test
    public void should_serialize_to_from_json() {
        GetConfigRequest1 req1 = GetConfigRequest1.sample();
        String json = gson.toJson(req1);
        GetConfigRequest1 req2 = gson.fromJson(json, GetConfigRequest1.class);
        assertThat(req1, is(req2));
    }

    @Test
    public void should_have_valid_sample() {
        GetConfigRequest1.sample();
    }
}