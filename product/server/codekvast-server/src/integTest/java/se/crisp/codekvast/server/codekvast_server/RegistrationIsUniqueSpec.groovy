package se.crisp.codekvast.server.codekvast_server

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.TestRestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.web.client.RestTemplate
import se.crisp.codekvast.server.codekvast_server.event.registration.IsNameUniqueRequest
import se.crisp.codekvast.server.codekvast_server.event.registration.IsNameUniqueResponse
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Olle Hallin
 */
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = CodekvastServerApplication.class)
@WebAppConfiguration
@IntegrationTest([
        'server.port=0',
        'management.port=0',
        'spring.datasource.url=jdbc:h2:mem:integrationTest',
        'codekvast.auto-register-customer=true',
])
public class RegistrationIsUniqueSpec extends Specification {

    @Value('${local.server.port}')
    private int port;

    RestTemplate template = new TestRestTemplate();

    @Unroll
    def "#kind '#name' is #unrollDescription"() {
        when:
        def entity = template.postForEntity("http://localhost:${port}/register/isUnique".toString(),
                IsNameUniqueRequest.builder().kind(kind).name(name).build(),
                IsNameUniqueResponse.class)

        then:
        entity.body.isUnique() == expected

        where:
        kind           | name             | expected
        'username'     | 'user'           | false
        'username'     | 'USer'           | false
        'username'     | 'USer '          | false
        'username'     | ' USer'          | false
        'username'     | 'userx'          | true
        'emailAddress' | 'user@demo.com'  | false
        'emailaddress' | 'user@demo.com'  | false
        'emailaddress' | 'user@demo.com ' | false
        'emailaddress' | 'user@demo.comx' | true
        'customerName' | "demo"           | false
        'customerName' | "demoX"          | true

        unrollDescription = expected ? "unique" : "not unique"
    }

}
