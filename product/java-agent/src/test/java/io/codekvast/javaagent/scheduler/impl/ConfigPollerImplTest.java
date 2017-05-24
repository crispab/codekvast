package io.codekvast.javaagent.scheduler.impl;

import com.google.gson.Gson;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class ConfigPollerImplTest {

    private final Gson gson = new Gson();

    @Test
    public void should_serialize_deserialize_getConfigRequest1() throws Exception {
        GetConfigRequest1 request1 = GetConfigRequest1.sample();
        String json = gson.toJson(request1);

        assertThat(json, containsString("\"licenseKey\":\"licenseKey\""));

        GetConfigRequest1 fromJson = gson.fromJson(json, GetConfigRequest1.class);
        assertThat(fromJson, is(request1));
    }

    @Test
    public void should_serialize_deserialize_GetConfigResponse1() throws Exception {
        GetConfigResponse1 response1 = GetConfigResponse1.sample();
        String json = gson.toJson(response1);

        assertThat(json, containsString("\"customerId\":-1"));

        GetConfigResponse1 fromJson = gson.fromJson(json, GetConfigResponse1.class);
        assertThat(fromJson, is(response1));

    }
}