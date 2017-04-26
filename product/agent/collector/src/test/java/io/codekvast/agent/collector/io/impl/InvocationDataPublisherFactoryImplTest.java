package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.collector.io.InvocationDataPublisherFactory;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class InvocationDataPublisherFactoryImplTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    @Rule
    public OutputCapture output = new OutputCapture();

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
    public void should_handle_file_system_name() throws Exception {
        // given
        InvocationDataPublisher publisher = factory.create(FileSystemInvocationDataPublisherImpl.NAME, config);

        // then
        assertThat(publisher, instanceOf(FileSystemInvocationDataPublisherImpl.class));
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
        output.expect(containsString("WARN"));
        output.expect(containsString("Unrecognized invocation data publisher name: 'foobar', will use no-op"));
    }
}
