package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.lib.codebase.CodeBaseFingerprint;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HttpInvocationDataPublisherImplTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();

    private File uploadedFile;

    private final HttpInvocationDataPublisherImpl publisher = new HttpInvocationDataPublisherImpl(config) {
        @Override
        void doPost(File file) {
            uploadedFile = file;
        }
    };

    @Test
    public void should_create_and_upload_file() throws Exception {
        Set<String> invocations = new HashSet<>(Arrays.asList("a", "b", "c"));
        publisher.setCodeBaseFingerprint(new CodeBaseFingerprint(1, "sha256"));
        publisher.doPublishInvocationData(System.currentTimeMillis(), invocations);

        assertThat(uploadedFile, notNullValue());
        assertThat(uploadedFile.getName(), startsWith("codekvast-invocations-"));
        assertThat(uploadedFile.getName(), endsWith(".ser"));
        assertThat(uploadedFile.exists(), is(false));
    }
}