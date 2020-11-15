package io.codekvast.javaagent.scheduler.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.gson.Gson;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import io.codekvast.javaagent.model.v2.GetConfigResponse2;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
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
  public void should_serialize_deserialize_getConfigRequest2() throws Exception {
    GetConfigRequest2 request2 = GetConfigRequest2.sample();
    String json = gson.toJson(request2);

    assertThat(json, containsString("\"licenseKey\":\"licenseKey\""));

    GetConfigRequest2 fromJson = gson.fromJson(json, GetConfigRequest2.class);
    assertThat(fromJson, is(request2));
  }

  @Test
  public void should_serialize_deserialize_GetConfigResponse1() throws Exception {
    GetConfigResponse1 response1 = GetConfigResponse1.sample();
    String json = gson.toJson(response1);

    assertThat(json, containsString("\"customerId\":1"));

    GetConfigResponse1 fromJson = gson.fromJson(json, GetConfigResponse1.class);
    assertThat(fromJson, is(response1));
  }

  @Test
  public void should_serialize_deserialize_GetConfigResponse2() throws Exception {
    GetConfigResponse2 response2 = GetConfigResponse2.sample();
    String json = gson.toJson(response2);

    assertThat(json, containsString("\"customerId\":1"));

    GetConfigResponse2 fromJson = gson.fromJson(json, GetConfigResponse2.class);
    assertThat(fromJson, is(response2));
  }
}
