package io.codekvast.javaagent.model.model.v1.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1;
import io.codekvast.javaagent.model.v2.GetConfigRequest2;
import org.junit.Test;

/**
 * Tests that Gson plays well together with the Lombok-generated stuff.
 *
 * @author olle.hallin@crisp.se
 */
public class GetConfigRequest2Test {
  private final Gson gson = new Gson();

  @Test
  public void should_serialize_to_from_json_1() {
    GetConfigRequest1 req1 = GetConfigRequest1.sample();
    String json = gson.toJson(req1);
    GetConfigRequest1 req2 = gson.fromJson(json, GetConfigRequest1.class);
    assertThat(req1, is(req2));
  }

  @Test
  public void should_serialize_to_from_json_2() {
    GetConfigRequest2 req1 = GetConfigRequest2.sample();
    String json = gson.toJson(req1);
    GetConfigRequest2 req2 = gson.fromJson(json, GetConfigRequest2.class);
    assertThat(req1, is(req2));
  }

  @Test
  public void should_convert_from_format1() {
    GetConfigRequest2 req2 =
        GetConfigRequest2.fromFormat1(GetConfigRequest1.sample(), "some-environment");
    assertThat(
        req2, is(GetConfigRequest2.sample().toBuilder().environment("some-environment").build()));
  }

  @Test
  public void should_have_valid_sample_1() {
    GetConfigRequest1.sample();
  }

  @Test
  public void should_have_valid_sample_2() {
    GetConfigRequest2.sample();
  }
}
