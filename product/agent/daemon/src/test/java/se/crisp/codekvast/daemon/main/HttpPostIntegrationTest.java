package se.crisp.codekvast.daemon.main;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import se.crisp.codekvast.daemon.CodekvastDaemon;

import java.lang.annotation.*;

/**
 * Meta annotation for integration tests against an embedded CodekvastDaemon application that uses HTTP POST as data processing strategy.
 *
 * @author olle.hallin@crisp.se
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringApplicationConfiguration(classes = CodekvastDaemon.class)
@IntegrationTest({
        "spring.profiles.active=httpPost",
        "spring.datasource.url=jdbc:h2:mem:integrationTest",
        "codekvast.apiAccessID=apiAccessID",
        "codekvast.apiAccessSecret=apiAccessSecret",
        "codekvast.serverUri=serverUri",
        "codekvast.dataPath=dataPath",
        "codekvast.serverUploadIntervalSeconds=600"
})
public @interface HttpPostIntegrationTest {
}
