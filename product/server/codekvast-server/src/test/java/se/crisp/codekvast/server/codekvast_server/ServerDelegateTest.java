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
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodeKvastServerMain.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest"})
public class ServerDelegateTest {

    @Value("${local.server.port}")
    private int port;

    @Inject
    private StorageService storageService;

    private ServerDelegate serverDelegate;

    private void createServerDelegate(String apiUsername, String apiPassword) throws URISyntaxException {
        serverDelegate = new ServerDelegateImpl(ServerDelegateConfig.builder()
                                                                    .customerName("customerName")
                                                                    .appName("appName")
                                                                    .appVersion("appVersion")
                                                                    .codeBaseName("codeBaseName")
                                                                    .environment("environment")
                                                                    .serverUri(new URI(String.format("http://localhost:%d", port)))
                                                                    .apiUsername(apiUsername)
                                                                    .apiPassword(apiPassword)
                                                                    .build());
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
    public void testCredentialsOkForUserNameSystem() throws URISyntaxException, ServerDelegateException {
        createServerDelegate("system", "0000");
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
    public void testUploadJvmRunData() throws ServerDelegateException, URISyntaxException {
        serverDelegate.uploadJvmRunData("hostName", System.currentTimeMillis(), System.currentTimeMillis(), UUID.randomUUID());
    }

    @Test
    public void testUploadSignatureData() throws ServerDelegateException, URISyntaxException {
        serverDelegate.uploadSignatureData(Arrays.asList("public String com.acme.Foo.foo()", "public void com.acme.Foo.bar()"));
    }

    @Test
    public void testUploadUsageData() throws ServerDelegateException, URISyntaxException {
        long now = System.currentTimeMillis();
        Collection<UsageDataEntry> usage = Arrays.asList(new UsageDataEntry("foo", now, UsageConfidence.EXACT_MATCH),
                                                         new UsageDataEntry("bar", now, UsageConfidence.EXACT_MATCH));
        serverDelegate.uploadUsageData(usage);
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
