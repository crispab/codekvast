package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import io.codekvast.junit5.extensions.CaptureSystemOutput;
import io.codekvast.junit5.extensions.CaptureSystemOutput.OutputCapture;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
@CaptureSystemOutput
class CodeBasePublisherFactoryImplTest {

  private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();
  private final CodeBasePublisherFactory factory = new CodeBasePublisherFactoryImpl();

  @Test
  void should_handle_noop_name(OutputCapture outputCapture) {
    // given
    CodeBasePublisher publisher = factory.create(NoOpCodeBasePublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
    outputCapture.expect(is(""));
  }

  @Test
  void should_warn_when_unrecognized_name(OutputCapture outputCapture) {
    // given
    CodeBasePublisher publisher = factory.create("foobar", config);

    // then
    assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
    outputCapture.expect(containsString("WARN"));
    outputCapture.expect(
        containsString("Unrecognized code base publisher name: 'foobar', will use no-op"));
  }
}
