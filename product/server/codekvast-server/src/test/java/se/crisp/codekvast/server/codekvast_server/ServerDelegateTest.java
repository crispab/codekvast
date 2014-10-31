package se.crisp.codekvast.server.codekvast_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import se.crisp.codekvast.server.agent.ServerDelegate;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;
import se.crisp.codekvast.server.agent.ServerDelegateException;
import se.crisp.codekvast.server.agent.impl.ServerDelegateImpl;
import se.crisp.codekvast.server.agent.model.v1.UsageConfidence;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

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
@SpringApplicationConfiguration(classes = CodekvastServerMain.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest",
                  "codekvast.auto-register-customer=true",
                  "codekvast.auto-register-application=true",
                 })
public class ServerDelegateTest {

    private static final String CUSTOMER_NAME = "customerName";
    private final String signature1 = "public String com.acme.Foo.foo()";
    private final String signature2 = "public void com.acme.Foo.bar()";
    private final String jvmFingerprint = UUID.randomUUID().toString();

    @Value("${local.server.port}")
    private int port;

    @Inject
    private StorageService storageService;

    @Inject
    private Validator validator;

    private ServerDelegate serverDelegate;

    private void createServerDelegate(String apiUsername, String apiPassword) throws URISyntaxException {
        serverDelegate = new ServerDelegateImpl(ServerDelegateConfig.builder()
                                                                    .customerName(CUSTOMER_NAME)
                                                                    .environment("environment")
                                                                    .serverUri(new URI(String.format("http://localhost:%d", port)))
                                                                    .apiUsername(apiUsername)
                                                                    .apiPassword(apiPassword)
                                                                    .build(), validator);
    }

    @Before
    public void beforeTest() throws URISyntaxException {
        createServerDelegate("agent", "0000");
    }

    @Test
    public void testCredentialsOkForUserNameAgent() throws ServerDelegateException, URISyntaxException {
        assertThat(serverDelegate.ping("Hello!"), is("You said Hello!"));
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
    public void testUploadSignatureData() throws ServerDelegateException, URISyntaxException, CodekvastException {
        // when
        serverDelegate.uploadJvmRunData("appName", "appVersion", "hostName", System.currentTimeMillis(), System.currentTimeMillis(),
                                        jvmFingerprint, "codekvastVersion", "codekvastVcsId");
        serverDelegate.uploadSignatureData(jvmFingerprint, Arrays.asList(signature1, signature2));

        // then
        assertThat(storageService.getSignatures(null), hasSize(2));
    }

    @Test
    public void testUploadUsageData() throws ServerDelegateException, URISyntaxException, CodekvastException {
        // Given
        long now = System.currentTimeMillis();
        Collection<UsageDataEntry> usage = Arrays.asList(new UsageDataEntry(signature1, now, UsageConfidence.EXACT_MATCH),
                                                         new UsageDataEntry(signature2, now, UsageConfidence.EXACT_MATCH));
        // when
        serverDelegate.uploadJvmRunData("appName", "appVersion", "hostName", System.currentTimeMillis(), System.currentTimeMillis(),
                                        jvmFingerprint, "codekvastVersion", "codekvastVcsId");
        serverDelegate.uploadUsageData(jvmFingerprint, usage);

        // then
        assertThat(storageService.getSignatures(CUSTOMER_NAME), hasSize(2));
        assertThat(storageService.getSignatures(CUSTOMER_NAME + "X"), hasSize(0));
    }

    private void assertThatPingThrowsHttpClientErrorException(String pingMessage, String expectedRootCauseMessage) {
        try {
            serverDelegate.ping(pingMessage);
            fail("Expected a ServerDelegateException");
        } catch (ServerDelegateException e) {
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
