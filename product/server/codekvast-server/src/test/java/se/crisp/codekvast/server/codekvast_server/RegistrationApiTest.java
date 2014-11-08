package se.crisp.codekvast.server.codekvast_server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.crisp.codekvast.server.codekvast_server.controller.RegistrationController;
import se.crisp.codekvast.server.codekvast_server.model.RegistrationRequest;
import se.crisp.codekvast.server.codekvast_server.model.RegistrationResponse;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CodekvastServerApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest",
                  "codekvast.auto-register-customer=true",
})
public class RegistrationApiTest {

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate = new RestTemplate();
    private URI registrationUri;

    @Before
    public void before() throws Exception {
        registrationUri = new URI(String.format("http://localhost:%d%s", port, RegistrationController.REGISTER_PATH));
    }

    @Test
    public void testRegistration1() {
        RegistrationRequest request = RegistrationRequest.builder().fullName("Full Name").username("username").emailAddress("foo@bar")
                                                         .password("pw").customerName("customerName").build();
        RegistrationResponse response = restTemplate.postForEntity(registrationUri, request, RegistrationResponse.class).getBody();
        assertThat(response.getGreeting(), is("Welcome Full Name!"));
    }

    @Test(expected = HttpClientErrorException.class)
    public void testRegistration2() {
        RegistrationRequest request = RegistrationRequest.builder().fullName("Full Name").username("user").emailAddress("foo@bar")
                                                         .password("pw").customerName("customerName2").build();
        restTemplate.postForEntity(registrationUri, request, RegistrationResponse.class).getBody();
    }

}
