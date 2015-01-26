package se.crisp.codekvast.server.codekvast_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.agent_api.AgentApi;
import se.crisp.codekvast.server.agent_api.AgentApiConfig;
import se.crisp.codekvast.server.agent_api.AgentApiException;
import se.crisp.codekvast.server.agent_api.impl.AgentApiImpl;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastServerTest
public class AgentApiTest {

    private final String signature1 = "public String com.acme.Foo.foo()";
    private final String signature2 = "public void com.acme.Foo.bar()";
    private final String jvmFingerprint = UUID.randomUUID().toString();

    @Value("${local.server.port}")
    private int port;

    @Inject
    private UserService userService;

    @Inject
    private Validator validator;

    private AgentApi agentApi;

    private void createServerDelegate(String apiAccessID, String apiAccessSecret) throws URISyntaxException {
        agentApi = new AgentApiImpl(AgentApiConfig.builder()
                                                  .serverUri(new URI(String.format("http://localhost:%d", port)))
                                                  .apiAccessID(apiAccessID)
                                                  .apiAccessSecret(apiAccessSecret)
                                                  .build(), validator);
    }

    @Before
    public void beforeTest() throws URISyntaxException {
        createServerDelegate("agent", "0000");
    }

    @Test
    public void testCredentialsOkForUserNameAgent() throws AgentApiException, URISyntaxException {
        assertThat(agentApi.ping("Hello!"), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkButInvalidPayload() throws URISyntaxException {
        assertThatPingThrowsHttpClientErrorException("Hello, Brave New World!", "412 Precondition Failed");
    }

    @Test
    public void testBadCredentials() throws URISyntaxException {
        createServerDelegate("agent", "0000foobar");
        assertThatPingThrowsHttpClientErrorException("Hello!", "401 Unauthorized");
    }

    @Test
    public void testCredentialsOkButWrongRole() throws URISyntaxException {
        createServerDelegate("user", "0000");
        assertThatPingThrowsHttpClientErrorException("Hello!", "403 Forbidden");
    }

    @Test
    public void testUploadSignatureData() throws AgentApiException, URISyntaxException, CodekvastException {
        // when

        agentApi.uploadJvmData(getJvmData());
        agentApi.uploadSignatureData(jvmFingerprint, Arrays.asList(signature1, signature2));

        // then
        assertThat(userService.getSignatures(null), hasSize(2));
    }

    @Test
    public void testUploadInvocationsData() throws AgentApiException, URISyntaxException, CodekvastException {
        // Given
        long now = System.currentTimeMillis();
        Collection<InvocationEntry> invocationEntries = Arrays.asList(new InvocationEntry(signature1, now, SignatureConfidence.EXACT_MATCH),
                                                                      new InvocationEntry(signature2, now,
                                                                                          SignatureConfidence.EXACT_MATCH));
        // when
        agentApi.uploadJvmData(getJvmData());
        agentApi.uploadInvocationsData(jvmFingerprint, invocationEntries);

        // then
        assertThat(userService.getSignatures("user"), hasSize(2));
        // TODO: assertThat(userService.getSignatures("userX"), hasSize(0));
    }

    private JvmData getJvmData() {
        return JvmData.builder()
                      .appName("appName")
                      .appVersion("appVersion")
                      .tags("tags")
                      .hostName("hostName")
                      .startedAtMillis(System.currentTimeMillis())
                      .dumpedAtMillis(System.currentTimeMillis())
                      .jvmFingerprint(jvmFingerprint)
                      .codekvastVersion("codekvastVersion")
                      .codekvastVcsId("codekvastVcsId")
                      .build();
    }

    private void assertThatPingThrowsHttpClientErrorException(String pingMessage, String expectedRootCauseMessage) {
        try {
            agentApi.ping(pingMessage);
            fail("Expected a AgentApiException");
        } catch (AgentApiException e) {
            assertThat(getRootCause(e).getMessage(), is(expectedRootCauseMessage));
        }
    }

    private Throwable getRootCause(Throwable t) {
        if (t.getCause() == null) {
            return t;
        }
        return getRootCause(t.getCause());
    }

}
