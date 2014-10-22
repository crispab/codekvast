package se.crisp.codekvast.server.codekvast_server;

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
import se.crisp.codekvast.server.codekvast_server.service.StorageService;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

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
                                                                    .environment("environment")
                                                                    .codeBaseName("codeBaseName")
                                                                    .serverUri(new URI(String.format("http://localhost:%d", port)))
                                                                    .apiUsername(apiUsername)
                                                                    .apiPassword(apiPassword)
                                                                    .build());
    }

    @Test
    public void testCredentialsOkForUserNameAgent() throws ServerDelegateException, URISyntaxException {
        createServerDelegate("agent", "0000");
        assertThat(serverDelegate.ping("Hello!"), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkForUserNameSystem() throws URISyntaxException, ServerDelegateException {
        createServerDelegate("system", "0000");
        assertThat(serverDelegate.ping("Hello!"), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkButInvalidPayload() throws URISyntaxException {
        createServerDelegate("agent", "0000");
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
    public void testUploadSignatures() throws ServerDelegateException, URISyntaxException {
        createServerDelegate("agent", "0000");
        serverDelegate.uploadSignatureData(Arrays.asList("com.acme.Foo.foo()", "com.acme.Foo.bar()"));
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
