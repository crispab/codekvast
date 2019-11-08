package io.codekvast.javaagent.model.model.v1.rest;

import com.google.gson.Gson;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests that Gson plays well together with the Lombok-generated stuff.
 *
 * @author olle.hallin@crisp.se
 */
public class GetConfigResponse1Test {
    private final Gson gson = new Gson();

    @Test
    public void should_serialize_to_from_json_1() {
        GetConfigResponse1 rsp1 = GetConfigResponse1.sample();
        String json = gson.toJson(rsp1);
        GetConfigResponse1 rsp2 = gson.fromJson(json, GetConfigResponse1.class);
        assertThat(rsp1, is(rsp2));
    }

    @Test
    public void should_serialize_to_from_json_2() {
        GetConfigResponse2 rsp1 = GetConfigResponse2.sample();
        String json = gson.toJson(rsp1);
        GetConfigResponse2 rsp2 = gson.fromJson(json, GetConfigResponse2.class);
        assertThat(rsp1, is(rsp2));
    }

    @Test
    public void should_convert_to_format_1() {
        GetConfigResponse1 rsp = GetConfigResponse2.toFormat1(GetConfigResponse2.sample());
        assertThat(rsp, is(GetConfigResponse1.sample()));
    }

    @Test
    public void should_have_valid_sample_1() {
        GetConfigResponse1.sample();
    }

    @Test
    public void should_have_valid_sample_2() {
        GetConfigResponse2.sample();
    }
}
