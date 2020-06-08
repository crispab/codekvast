package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

/** @author olle.hallin@crisp.se */
public class InvocationDataPublisherFactoryImplTest {

  private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();

  @Rule public OutputCaptureRule output = new JulAwareOutputCapture();

  private final InvocationDataPublisherFactory factory = new InvocationDataPublisherFactoryImpl();

  @Test
  public void should_handle_noop_name() {
    // given
    InvocationDataPublisher publisher =
        factory.create(NoOpInvocationDataPublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
    output.expect(is(""));
  }

  @Test
  public void should_handle_http_name() {
    // given
    InvocationDataPublisher publisher =
        factory.create(HttpInvocationDataPublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(HttpInvocationDataPublisherImpl.class));
    output.expect(is(""));
  }

  @Test
  public void should_warn_when_unrecognized_name() {
    // given
    InvocationDataPublisher publisher = factory.create("foobar", config);

    // then
    assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
    output.expect(containsString("[WARNING]"));
    output.expect(
        containsString("Unrecognized invocation data publisher name: 'foobar', will use no-op"));
  }
}
