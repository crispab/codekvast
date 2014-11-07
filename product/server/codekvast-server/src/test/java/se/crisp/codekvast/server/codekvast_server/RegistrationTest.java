package se.crisp.codekvast.server.codekvast_server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import se.crisp.codekvast.server.codekvast_server.model.IsNameUniqueRequest;
import se.crisp.codekvast.server.codekvast_server.model.IsNameUniqueResponse;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodekvastServerApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest",
                  "codekvast.auto-register-customer=false",
})
public class RegistrationTest {

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testIsUsernameUnique1() throws Exception {
        ResponseEntity<IsNameUniqueResponse> responseEntity =
                restTemplate.postForEntity(new URI(String.format("http://localhost:%d%s", port, "/register/isUnique")),
                                           IsNameUniqueRequest.builder().kind("username").name("user").build(),
                                           IsNameUniqueResponse.class);
        assertThat(responseEntity.getBody().isUnique(), is(false));
    }

}
