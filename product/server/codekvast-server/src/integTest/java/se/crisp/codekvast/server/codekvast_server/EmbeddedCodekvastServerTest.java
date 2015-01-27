package se.crisp.codekvast.server.codekvast_server;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.lang.annotation.*;

/**
 * Meta annotation for integration tests against an embedded CodekvastServerApplication
 *
 * @author Olle Hallin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringApplicationConfiguration(classes = CodekvastServerApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.thymeleaf.cache=true",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest",
})
public @interface EmbeddedCodekvastServerTest {
}
