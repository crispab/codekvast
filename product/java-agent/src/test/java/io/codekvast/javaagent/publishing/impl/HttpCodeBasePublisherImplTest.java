package io.codekvast.javaagent.publishing.impl;

import io.codekvast.javaagent.codebase.CodeBase;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v1.MethodSignature1;
import io.codekvast.javaagent.model.v1.SignatureStatus1;
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

    private final AgentConfig config = AgentConfigFactory
        .createSampleAgentConfig().toBuilder()
        .appName("appName").appVersion("appVersion").build();
    private final CodeBase codeBase = new CodeBase(config);
    private final HttpCodeBasePublisherImpl publisher = new TestableHttpCodeBasePublisherImpl();

    private File uploadedFile;
    private int uploadedPublicationSize;

    @Test
    public void should_create_and_upload_file() throws Exception {
        // given
        codeBase.getSignatures().put("key", MethodSignature1.createSampleMethodSignature());
        codeBase.getStatuses().put("key", SignatureStatus1.NOT_INVOKED);

        // when
        publisher.doPublishCodeBase(codeBase);

        // then
        assertThat(uploadedFile, notNullValue());
        assertThat(uploadedFile.getName(), startsWith("codebase-appname-appversion-"));
        assertThat(uploadedFile.getName(), endsWith(".ser"));
        assertThat(uploadedFile.exists(), is(false));

        assertThat(uploadedPublicationSize, is(1));
    }

    @SuppressWarnings("ClassTooDeepInInheritanceTree")
    private class TestableHttpCodeBasePublisherImpl extends HttpCodeBasePublisherImpl {

        TestableHttpCodeBasePublisherImpl() {
            super(HttpCodeBasePublisherImplTest.this.config);
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