package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
@CaptureSystemOutput
class InvocationDataPublisherFactoryImplTest {

  private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();
  private final InvocationDataPublisherFactory factory = new InvocationDataPublisherFactoryImpl();

  @Test
  void should_handle_noop_name(OutputCapture outputCapture) {
    // given
    InvocationDataPublisher publisher =
        factory.create(NoOpInvocationDataPublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
    outputCapture.expect(is(""));
  }

  @Test
  void should_handle_http_name(OutputCapture outputCapture) {
    // given
    InvocationDataPublisher publisher =
        factory.create(HttpInvocationDataPublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(HttpInvocationDataPublisherImpl.class));
    outputCapture.expect(is(""));
  }

  @Test
  void should_warn_when_unrecognized_name(OutputCapture outputCapture) {
    // given
    InvocationDataPublisher publisher = factory.create("foobar", config);

    // then
    assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
    outputCapture.expect(containsString("WARN"));
    outputCapture.expect(
        containsString("Unrecognized invocation data publisher name: 'foobar', will use no-op"));
  }
}
