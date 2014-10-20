package se.crisp.codekvast.server.codekvast_server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.crisp.codekvast.server.agent.AgentRestEndpoints;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;
import se.crisp.codekvast.server.agent.impl.ServerDelegateImpl;
import se.crisp.codekvast.server.agent.model.test.Ping;
import se.crisp.codekvast.server.agent.model.test.Pong;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodeKvastServerMain.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integTest"})
public class ServerDelegateTest {

    @Value("${local.server.port}")
    private int port;

    private ServerDelegateConfig config;
    private ServerDelegateImpl serverDelegate;
    private RestTemplate restTemplate;

    private void createCollaborators(String apiUsername, String apiPassword) throws URISyntaxException {
        config = ServerDelegateConfig.builder()
                                     .customerName("customerName")
                                     .appName("appName")
                                     .appVersion("appVersion")
                                     .environment("environment")
                                     .codeBaseName("codeBaseName")
                                     .serverUri(new URI(String.format("http://localhost:%d", port)))
                                     .apiUsername(apiUsername)
                                     .apiPassword(apiPassword)
                                     .build();

        serverDelegate = new ServerDelegateImpl(config);
        restTemplate = serverDelegate.getRestTemplate();
    }

    @Test
    public void testCredentialsOk() throws URISyntaxException {
        createCollaborators("agent", "0000");
        ResponseEntity<Pong> response = restTemplate.postForEntity(config.getServerUri() + AgentRestEndpoints.PING,
                                                                   Ping.builder().message("Hello!").build(),
                                                                   Pong.class);
        assertThat(response.getBody().getMessage(), is("Hello!"));
    }

    @Test
    public void testCredentialsOkButTooLongPingMessage() throws URISyntaxException {
        createCollaborators("agent", "0000");
        assertThatPingThrows("Hello, World!", "400 Bad Request");
    }

    @Test
    public void testBadCredentials() throws URISyntaxException {
        createCollaborators("agent", "0000foobar");
        assertThatPingThrows("Hello!", "401 Unauthorized");
    }

    @Test
    public void testCredentialsOkButWrongRole() throws URISyntaxException {
        createCollaborators("user", "0000");
        assertThatPingThrows("Hello!", "403 Forbidden");
    }

    private void assertThatPingThrows(String pingMessage, String expectedExceptionMessage) {
        try {
            restTemplate.postForEntity(config.getServerUri() + AgentRestEndpoints.PING,
                                       Ping.builder().message(pingMessage).build(),
                                       Pong.class);
            fail("Expected an HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertThat(e.getMessage(), is(expectedExceptionMessage));
        }
    }

}
