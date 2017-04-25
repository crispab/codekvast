package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.collector.io.CodeBasePublisher;
import io.codekvast.agent.collector.io.CodeBasePublisherFactory;
import io.codekvast.agent.collector.io.impl.CodeBasePublisherFactoryImpl;
import io.codekvast.agent.collector.io.impl.NoOpCodeBasePublisherImpl;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class CodeBasePublisherFactoryImplTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

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
    public void should_handle_filesystem_name() throws Exception {
        // given
        CodeBasePublisher publisher = factory.create(FileSystemCodeBasePublisherImpl.NAME, config);

        // then
        assertThat(publisher, instanceOf(FileSystemCodeBasePublisherImpl.class));
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
