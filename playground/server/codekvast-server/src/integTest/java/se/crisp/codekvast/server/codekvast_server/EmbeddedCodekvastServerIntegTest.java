package se.crisp.codekvast.server.codekvast_server;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import java.lang.annotation.*;

/**
 * Meta annotation for integration tests against an embedded CodekvastServerApplication
 *
 * @author olle.hallin@crisp.se
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringApplicationConfiguration(classes = CodekvastServerApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0",
                  "management.port=0",
                  "spring.datasource.url=jdbc:h2:mem:integrationTest",
                  "codekvast.backupPath=build/backup",
                  "codekvast.backupSchedule=0 0 4 * * *"
})
public @interface EmbeddedCodekvastServerIntegTest {
}
