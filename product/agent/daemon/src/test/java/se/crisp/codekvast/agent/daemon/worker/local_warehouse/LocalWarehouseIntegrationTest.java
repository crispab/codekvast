package se.crisp.codekvast.agent.daemon.worker.local_warehouse;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import se.crisp.codekvast.agent.daemon.CodekvastDaemon;

import java.lang.annotation.*;

/**
 * Meta annotation for integration tests against an embedded CodekvastDaemon application that uses a local warehouse as data processing
 * strategy.
 *
 * @author olle.hallin@crisp.se
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringApplicationConfiguration(classes = CodekvastDaemon.class)
@IntegrationTest({
        "spring.datasource.url=jdbc:h2:mem:integrationTest",
        "codekvast.apiAccessID=apiAccessID",
        "codekvast.apiAccessSecret=apiAccessSecret",
        "codekvast.serverUri=serverUri",
        "codekvast.dataPath=dataPath",
        "codekvast.dataProcessingIntervalSeconds=600",
        "codekvast.environment=integration-test",
        "codekvast.exportFile=/tmp/codekvast-data.zip"
})
public @interface LocalWarehouseIntegrationTest {
}
