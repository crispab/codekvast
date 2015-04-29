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
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.model.event.display.SignatureDisplay;
import se.crisp.codekvast.server.codekvast_server.service.UserService;

import javax.inject.Inject;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence.EXACT_MATCH;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastServerIntegTest
public class AgentApiIntegTest {

    private static final int SIGNATURES_SIZE = 1300;

    private final String jvmUuid = UUID.randomUUID().toString();
    private final Random random = new Random();
    private final List<String> signatures = getRandomSignatures(SIGNATURES_SIZE);

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
    public void testUploadSignatures() throws AgentApiException, URISyntaxException, CodekvastException {
        // when
        JvmData jvmData = getJvmData();
        agentApi.uploadJvmData(jvmData);
        agentApi.uploadSignatureData(jvmData, signatures);

        // then
        assertThat(userService.getSignatures("user"), hasSize(SIGNATURES_SIZE));

        // given
        long now = System.currentTimeMillis();
        List<SignatureEntry> invocationEntries = asList(new SignatureEntry(signatures.get(1), now, 100L, EXACT_MATCH),
                                                        new SignatureEntry(signatures.get(2), now, 100L, EXACT_MATCH),
                                                        new SignatureEntry(signatures.get(2), now + 1000L, 1100L, EXACT_MATCH));
        // when
        agentApi.uploadInvocationData(jvmData, invocationEntries);

        // then
        Collection<SignatureDisplay> actual = userService.getSignatures("user");
        assertThat(actual, hasSize(SIGNATURES_SIZE));
        for (SignatureDisplay entry : actual) {
            if (entry.getName().equals(signatures.get(0))) {
                assertThat(entry.getInvokedAtMillis(), is(0L));
            }
            if (entry.getName().equals(signatures.get(1))) {
                assertThat((double) entry.getInvokedAtMillis(), closeTo(now, 100D));
            }
            if (entry.getName().equals(signatures.get(2))) {
                assertThat((double) entry.getInvokedAtMillis(), closeTo(now + 1000D, 100D));
            }
        }

        // assert that only signatures from the own organisation is returned...
        assertThat(userService.getSignatures("system"), hasSize(0));
    }

    private List<String> getRandomSignatures(int size) {
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(getRandomString(100));
        }
        return result;
    }

    private String getRandomString(int meanSize) {
        StringBuilder sb = new StringBuilder();
        int len = Math.max(10, meanSize / 2 + (int) (random.nextGaussian() * meanSize));
        for (int i = 0; i < len; i++) {
            char c = (char) ('a' + random.nextInt(25));
            sb.append(c);
        }
        return sb.toString();
    }

    private JvmData getJvmData() {
        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .agentUploadIntervalSeconds(300)
                      .agentVcsId("agentVcsId")
                      .agentVersion("agentVersion")
                      .agentTimeMillis(System.currentTimeMillis())
                      .appName("appName")
                      .appVersion("appVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .collectorVcsId("collectorVcsId")
                      .collectorVersion("collectorVersion")
                      .dumpedAtMillis(System.currentTimeMillis())
                      .jvmUuid(jvmUuid)
                      .methodVisibility("methodVisibility")
                      .startedAtMillis(System.currentTimeMillis())
                      .tags("tags")
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
