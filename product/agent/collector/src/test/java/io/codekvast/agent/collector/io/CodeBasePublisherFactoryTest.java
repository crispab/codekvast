package io.codekvast.agent.collector.io;

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
public class CodeBasePublisherFactoryTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    @Rule
    public OutputCapture output = new OutputCapture();

    @Test
    public void should_handle_noop_name() throws Exception {
        // given
        CodeBasePublisher publisher = CodeBasePublisherFactory.create(NoOpCodeBasePublisherImpl.NAME, config);

        // then
        assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
        output.expect(is(""));
    }

    @Test
    public void should_warn_when_unrecognized_name() throws Exception {
        // given
        CodeBasePublisher publisher = CodeBasePublisherFactory.create("foobar", config);

        // then
        assertThat(publisher, instanceOf(NoOpCodeBasePublisherImpl.class));
        output.expect(containsString("WARN"));
        output.expect(containsString("Unrecognized code base publisher name: 'foobar', will use no-op"));
    }
}
