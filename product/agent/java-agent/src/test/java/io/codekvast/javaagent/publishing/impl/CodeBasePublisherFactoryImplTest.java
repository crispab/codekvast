package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

/** @author olle.hallin@crisp.se */
public class CodeBasePublisherFactoryImplTest {

  private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();

  @Rule public OutputCaptureRule output = new JulAwareOutputCapture();

  private final CodeBasePublisherFactory factory = new CodeBasePublisherFactoryImpl();

  @Test
  public void should_handle_noop_name() throws Exception {
    // given
    CodeBasePublisher publisher = factory.create(NoOpCodeBasePublisherImpl.NAME, config);

    // then
    assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
    output.expect(is(""));
  }

  @Test
  public void should_warn_when_unrecognized_name() throws Exception {
    // given
    CodeBasePublisher publisher = factory.create("foobar", config);

    // then
    assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
    output.expect(containsString("[WARNING]"));
    output.expect(
        containsString("Unrecognized code base publisher name: 'foobar', will use no-op"));
  }
}
