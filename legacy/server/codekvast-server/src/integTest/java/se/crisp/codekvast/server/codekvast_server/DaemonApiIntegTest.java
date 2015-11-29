package se.crisp.codekvast.server.codekvast_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import se.crisp.codekvast.server.daemon_api.DaemonApi;
import se.crisp.codekvast.server.daemon_api.DaemonApiConfig;
import se.crisp.codekvast.server.daemon_api.DaemonApiException;
import se.crisp.codekvast.server.daemon_api.impl.DaemonApiImpl;
import se.crisp.codekvast.server.daemon_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import javax.inject.Inject;
import javax.validation.Validator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@EmbeddedCodekvastServerIntegTest
public class DaemonApiIntegTest {

    private static final int SIGNATURES_SIZE = 1300;

    private final String jvmUuid = UUID.randomUUID().toString();
    private final Random random = new Random();
    private final List<String> signatures = getRandomSignatures(SIGNATURES_SIZE);

    @Value("${local.server.port}")
    private int port;

    @Inject
    private Validator validator;

    private DaemonApi daemonApi;

    private void createServerDelegate(String apiAccessID, String apiAccessSecret) throws URISyntaxException {
        daemonApi = new DaemonApiImpl(DaemonApiConfig.builder()
                                                  .serverUri(new URI(String.format("http://localhost:%d", port)))
                                                  .apiAccessID(apiAccessID)
                                                  .apiAccessSecret(apiAccessSecret)
                                                  .build(), validator);
    }

    @Before
    public void beforeTest() throws URISyntaxException {
        createServerDelegate("daemon", "0000");
    }

    @Test
    public void testCredentialsOkForUserNameAgent() throws DaemonApiException, URISyntaxException {
        assertThat(daemonApi.ping("Hello!"), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkButInvalidPayload() throws URISyntaxException {
        assertThatPingThrowsHttpClientErrorException("Hello, Brave New World!", "412 Precondition Failed");
    }

    @Test
    public void testBadCredentials() throws URISyntaxException {
        createServerDelegate("daemon", "0000foobar");
        assertThatPingThrowsHttpClientErrorException("Hello!", "401 Unauthorized");
    }

    @Test
    public void testCredentialsOkButWrongRole() throws URISyntaxException {
        createServerDelegate("user", "0000");
        assertThatPingThrowsHttpClientErrorException("Hello!", "403 Forbidden");
    }

    @Test
    public void testUploadSignatures() throws DaemonApiException, URISyntaxException, CodekvastException {
        // when
        JvmData jvmData = getJvmData();
        daemonApi.uploadJvmData(jvmData);
        daemonApi.uploadSignatureData(jvmData, signatures);
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

    @SuppressWarnings("unused")
    private JvmData getJvmData() {
        return JvmData.builder()
                      .daemonComputerId("daemonComputerId")
                      .daemonHostName("daemonHostName")
                      .daemonUploadIntervalSeconds(300)
                      .daemonVcsId("daemonVcsId")
                      .daemonVersion("daemonVersion")
                      .daemonTimeMillis(System.currentTimeMillis())
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
            daemonApi.ping(pingMessage);
            fail("Expected a DaemonApiException");
        } catch (DaemonApiException e) {
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
