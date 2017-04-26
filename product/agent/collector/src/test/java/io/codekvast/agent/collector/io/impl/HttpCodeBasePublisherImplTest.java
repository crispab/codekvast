package io.codekvast.agent.collector.io.impl;

import io.codekvast.agent.lib.codebase.CodeBase;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.config.CollectorConfigFactory;
import okhttp3.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
public class HttpCodeBasePublisherImplTest {

    private final CollectorConfig config = CollectorConfigFactory.createSampleCollectorConfig();
    private final CodeBase codeBase = new CodeBase(config);
    private final HttpCodeBasePublisherImpl publisher = new TestableHttpCodeBasePublisherImpl();

    private File uploadedFile;

    @Test
    public void should_create_and_upload_file() throws Exception {
        // given
        assertThat(publisher.getCodeBaseFingerprint(), nullValue());

        // when
        publisher.doPublishCodeBase(codeBase);

        // then
        assertThat(uploadedFile, notNullValue());
        assertThat(uploadedFile.getName(), startsWith("codekvast-codebase-"));
        assertThat(uploadedFile.getName(), endsWith(".ser"));
        assertThat(uploadedFile.exists(), is(false));
    }

    @SuppressWarnings("ClassTooDeepInInheritanceTree")
    private class TestableHttpCodeBasePublisherImpl extends HttpCodeBasePublisherImpl {

        TestableHttpCodeBasePublisherImpl() {
            super(HttpCodeBasePublisherImplTest.this.config);
        }

        @Override
        void doPost(File file, String url, String fingerprint) throws IOException {
            super.doPost(file, url, fingerprint);
            uploadedFile = file;
        }

        @Override
        Response executeRequest(Request request) throws IOException {
            return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .body(ResponseBody.create(MediaType.parse("text/plain"), "OK"))
                .build();
        }

    }
}