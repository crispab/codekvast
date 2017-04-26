package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HttpCodeBasePublisherImplTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private File uploadedFile;

    private final HttpCodeBasePublisherImpl publisher = new HttpCodeBasePublisherImpl(config) {
        @Override
        void doPut(File file) {
            uploadedFile = file;
        }
    };

    @Test
    public void should_create_and_upload_file() throws Exception {
        CodeBase codeBase = new CodeBase(config);

        publisher.doPublishCodeBase(codeBase);

        assertThat(uploadedFile, notNullValue());
        assertThat(uploadedFile.getName(), containsString("codekvast-codebase"));
        assertThat(uploadedFile.getName(), containsString(".ser"));
        assertThat(uploadedFile.exists(), is(false));
    }
}