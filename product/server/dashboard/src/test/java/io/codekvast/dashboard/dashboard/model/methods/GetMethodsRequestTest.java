package io.codekvast.dashboard.dashboard.model.methods;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
public class GetMethodsRequestTest {

  @Test
  public void should_normalize_signature_containing_percent_sequence() {
    GetMethodsRequest req = GetMethodsRequest.defaults().toBuilder().signature("%%%").build();
    assertThat(req.getNormalizedSignature(), is("%"));
  }

  @Test
  public void should_normalize_signature_containing_no_wildcards() {
    GetMethodsRequest req = GetMethodsRequest.defaults().toBuilder().signature("foobar").build();
    assertThat(req.getNormalizedSignature(), is("foobar%"));
  }

  @Test
  public void should_normalize_signature_containing_asterisk() {
    GetMethodsRequest req = GetMethodsRequest.defaults().toBuilder().signature("foo*bar").build();
    assertThat(req.getNormalizedSignature(), is("foo%bar%"));
  }

  @Test
  public void should_normalize_signature_containing_question_mark() {
    GetMethodsRequest req = GetMethodsRequest.defaults().toBuilder().signature("foo?bar").build();
    assertThat(req.getNormalizedSignature(), is("foo_bar%"));
  }
}
