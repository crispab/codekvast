package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.publishing.CodeBasePublisher;
import io.codekvast.javaagent.publishing.CodeBasePublisherFactory;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBasePublisherFactoryImplTest {

    private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();

    @Rule
    public OutputCapture output = new OutputCapture();

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
        output.expect(containsString("WARN"));
        output.expect(containsString("Unrecognized code base publisher name: 'foobar', will use no-op"));
    }
}
