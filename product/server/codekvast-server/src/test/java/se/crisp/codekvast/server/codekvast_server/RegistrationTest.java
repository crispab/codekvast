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
import se.crisp.codekvast.server.codekvast_server.model.IsNameUniqueRequest;
import se.crisp.codekvast.server.codekvast_server.model.IsNameUniqueResponse;
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
public class RegistrationTest {

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate = new RestTemplate();
    private URI isUniqueUri;
    private URI registrationUri;

    @Before
    public void before() throws Exception {
        isUniqueUri = new URI(String.format("http://localhost:%d%s", port, "/register/isUnique"));
        registrationUri = new URI(String.format("http://localhost:%d%s", port, "/register"));
    }

    @Test
    public void testIsUsernameUnique1() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("username").name("user").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(false));
    }

    @Test
    public void testIsUsernameUnique2() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("username").name("User").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(false));
    }

    @Test
    public void testIsUsernameUnique3() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("username").name("User1").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(true));
    }

    @Test
    public void testIsUsernameUnique4() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("customername").name("demo").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(false));
    }

    @Test
    public void testIsUsernameUnique5() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("customername").name("dem").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(true));
    }

    @Test
    public void testIsUsernameUnique6() throws Exception {
        assertThat(restTemplate.postForEntity(isUniqueUri,
                                              IsNameUniqueRequest.builder().kind("customername").name("demoo").build(),
                                              IsNameUniqueResponse.class).getBody().isUnique(), is(true));
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
