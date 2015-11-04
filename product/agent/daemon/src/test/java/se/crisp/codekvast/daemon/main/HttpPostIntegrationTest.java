package se.crisp.codekvast.daemon.main;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import se.crisp.codekvast.daemon.CodekvastDaemon;

import java.lang.annotation.*;

import static se.crisp.codekvast.daemon.DaemonConstants.HTTP_POST_PROFILE;

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
        "spring.profiles.active=" + HTTP_POST_PROFILE,
        "spring.datasource.url=jdbc:h2:mem:integrationTest",
        "codekvast.apiAccessID=apiAccessID",
        "codekvast.apiAccessSecret=apiAccessSecret",
        "codekvast.serverUri=serverUri",
        "codekvast.dataPath=dataPath",
        "codekvast.dataProcessingIntervalSeconds=600",
        "codekvast.environment=" + HTTP_POST_PROFILE + "-integration-test"
})
public @interface HttpPostIntegrationTest {
}
