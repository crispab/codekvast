package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.codebase.CodeBaseFingerprint;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import okhttp3.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HttpInvocationDataPublisherImplTest {

    private final AgentConfig config = AgentConfigFactory
        .createSampleAgentConfig().toBuilder().appName("appName").appVersion("appVersion").build();

    private File uploadedFile;
    private int uploadedPublicationSize;

    private final HttpInvocationDataPublisherImpl publisher = new TestableHttpInvocationDataPublisherImpl();

    @Test
    public void should_create_and_upload_file_when_invocations_exist() throws Exception {
        Set<String> invocations = new HashSet<>(Arrays.asList("a", "b", "c"));
        publisher.setCodeBaseFingerprint(CodeBaseFingerprint.builder(config).build());
        publisher.doPublishInvocationData(System.currentTimeMillis(), invocations);

        assertThat(uploadedFile, notNullValue());
        assertThat(uploadedFile.getName(), startsWith("invocations-appname-appversion-"));
        assertThat(uploadedFile.getName(), endsWith(".ser"));
        assertThat(uploadedFile.exists(), is(false));

        assertThat(uploadedPublicationSize, is(invocations.size()));
    }

    @Test
    public void should_not_create_and_upload_file_when_no_invocations_exist() throws Exception {
        Set<String> invocations = new HashSet<>();
        publisher.setCodeBaseFingerprint(CodeBaseFingerprint.builder(config).build());
        publisher.doPublishInvocationData(System.currentTimeMillis(), invocations);

        assertThat(uploadedFile, nullValue());
        assertThat(uploadedPublicationSize, is(0));
    }

    private class TestableHttpInvocationDataPublisherImpl extends HttpInvocationDataPublisherImpl {

        TestableHttpInvocationDataPublisherImpl() {
            super(HttpInvocationDataPublisherImplTest.this.config);
        }

        @Override
        void doPost(File file, String url, String fingerprint, int publicationSize) throws IOException {
            super.doPost(file, url, fingerprint, publicationSize);
            uploadedFile = file;
            uploadedPublicationSize = publicationSize;
        }

        @Override
        Response executeRequest(Request request) throws IOException {
            return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("text/plain"), "OK"))
                .build();
        }
    }
}
