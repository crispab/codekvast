package io.codekvast.javaagent.publishing.impl;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codekvast.javaagent.codebase.CodeBase;
import io.codekvast.javaagent.config.AgentConfig;
import io.codekvast.javaagent.config.AgentConfigFactory;
import io.codekvast.javaagent.model.v3.MethodSignature3;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

/** @author olle.hallin@crisp.se */
public class HttpCodeBasePublisherImplTest {

  private final AgentConfig config =
      AgentConfigFactory.createSampleAgentConfig().toBuilder()
          .appName("appName")
          .appVersion("appVersion")
          .build();
  private final CodeBase codeBase = new CodeBase(config);
  private final HttpCodeBasePublisherImpl publisher = new TestableHttpCodeBasePublisherImpl();

  private File uploadedFile;
  private int uploadedPublicationSize;

  @Test
  public void should_create_and_upload_file() throws Exception {
    // given
    codeBase.getSignatures().add(MethodSignature3.createSampleMethodSignature());

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
    Response executeRequest(Request request) {
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
