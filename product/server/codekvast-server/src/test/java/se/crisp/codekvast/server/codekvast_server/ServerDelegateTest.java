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

        restTemplate = new ServerDelegateImpl(config).getRestTemplate();
    }

    @Test
    public void testCredentialsOkForUserNameAgent() throws URISyntaxException {
        createCollaborators("agent", "0000");
        ResponseEntity<Pong> response = restTemplate.postForEntity(config.getServerUri() + AgentRestEndpoints.PING,
                                                                   Ping.builder().message("Hello!").build(),
                                                                   Pong.class);
        assertThat(response.getBody().getMessage(), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkForUserNameSystem() throws URISyntaxException {
        createCollaborators("system", "0000");
        ResponseEntity<Pong> response = restTemplate.postForEntity(config.getServerUri() + AgentRestEndpoints.PING,
                                                                   Ping.builder().message("Hello!").build(),
                                                                   Pong.class);
        assertThat(response.getBody().getMessage(), is("You said Hello!"));
    }

    @Test
    public void testCredentialsOkButInvalidPayload() throws URISyntaxException {
        createCollaborators("agent", "0000");
        assertThatPingThrowsHttpClientErrorException("Hello, Brave New World!", "400 Bad Request");
    }

    @Test
    public void testBadCredentials() throws URISyntaxException {
        createCollaborators("agent", "0000foobar");
        assertThatPingThrowsHttpClientErrorException("Hello!", "401 Unauthorized");
    }

    @Test
    public void testCredentialsOkButWrongRole() throws URISyntaxException {
        createCollaborators("user", "0000");
        assertThatPingThrowsHttpClientErrorException("Hello!", "403 Forbidden");
    }

    private void assertThatPingThrowsHttpClientErrorException(String pingMessage, String expectedExceptionMessage) {
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
