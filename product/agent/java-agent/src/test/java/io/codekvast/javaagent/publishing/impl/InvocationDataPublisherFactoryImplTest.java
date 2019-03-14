package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.publishing.InvocationDataPublisher;
import io.codekvast.javaagent.publishing.InvocationDataPublisherFactory;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class InvocationDataPublisherFactoryImplTest {

    private final AgentConfig config = AgentConfigFactory.createSampleAgentConfig();

    @Rule
    public OutputCapture output = new JulAwareOutputCapture();

    private final InvocationDataPublisherFactory factory = new InvocationDataPublisherFactoryImpl();

    @Test
    public void should_handle_noop_name() throws Exception {
        // given
        InvocationDataPublisher publisher = factory.create(NoOpInvocationDataPublisherImpl.NAME, config);

        // then
        assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
        output.expect(is(""));
    }

    @Test
    public void should_handle_http_name() throws Exception {
        // given
        InvocationDataPublisher publisher = factory.create(HttpInvocationDataPublisherImpl.NAME, config);

        // then
        assertThat(publisher, instanceOf(HttpInvocationDataPublisherImpl.class));
        output.expect(is(""));
    }

    @Test
    public void should_warn_when_unrecognized_name() throws Exception {
        // given
        InvocationDataPublisher publisher = factory.create("foobar", config);

        // then
        assertThat(publisher, instanceOf(NoOpInvocationDataPublisherImpl.class));
        output.expect(containsString("[WARNING]"));
        output.expect(containsString("Unrecognized invocation data publisher name: 'foobar', will use no-op"));
    }
}
