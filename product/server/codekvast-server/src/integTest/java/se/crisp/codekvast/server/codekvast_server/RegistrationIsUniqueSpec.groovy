package se.crisp.codekvast.server.codekvast_server

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.TestRestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import static se.crisp.codekvast.server.codekvast_server.controller.RegistrationController.IsNameUniqueRequest
import static se.crisp.codekvast.server.codekvast_server.controller.RegistrationController.IsNameUniqueResponse

/**
 * @author olle.hallin@crisp.se
 */
@EmbeddedCodekvastServerTest
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = CodekvastServerApplication.class)
public class RegistrationIsUniqueSpec extends Specification {

    @Value('${local.server.port}')
    private int port;

    RestTemplate template = new TestRestTemplate();

    @Unroll
    def "'#kind' '#name' should #unrollDescription"() {
        when:
        def entity = template.postForEntity("http://localhost:${port}/register/isUnique".toString(),
                IsNameUniqueRequest.builder().kind(kind).name(name).build(),
                IsNameUniqueResponse.class)

        then:
        entity.body.isUnique() == expected

        where:
        kind               | name             | expected
        'username'         | 'user'           | false
        'username'         | 'USer'           | false
        'userName'         | 'USer'           | false
        'username'         | 'USer '          | false
        'username'         | ' USer'          | false
        'username'         | 'userx'          | true
        'emailAddress'     | 'user@demo.com'  | false
        'emailaddress'     | 'user@demo.com'  | false
        'emailaddress'     | 'user@demo.com ' | false
        'emailaddress'     | 'user@demo.comx' | true
        'organisationName' | "demo"           | false
        'organisationName' | "Demo" | false
        'organisAtionname' | "demo"           | false
        'organisationName' | "demoX"          | true

        unrollDescription = expected ? "be unique" : "not be unique"
    }

}
