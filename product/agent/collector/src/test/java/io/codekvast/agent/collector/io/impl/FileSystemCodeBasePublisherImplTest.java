package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * @author olle.hallin@crisp.se
 */
public class FileSystemCodeBasePublisherImplTest {

    private CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private final FileSystemCodeBasePublisherImpl publisher = new FileSystemCodeBasePublisherImpl(config);

    @Test
    public void should_expand_hostname_placeholder() throws Exception {
        File file = new File("/tmp/foo-#hostname#.ser");
        File expanded = publisher.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }

    @Test
    public void should_expand_timestamp_placeholder() throws Exception {
        File file = new File("/tmp/foo-#timestamp#.ser");
        File expanded = publisher.expandPlaceholders(file);

        assertThat(expanded.getName(), not(is(file.getName())));
    }

}