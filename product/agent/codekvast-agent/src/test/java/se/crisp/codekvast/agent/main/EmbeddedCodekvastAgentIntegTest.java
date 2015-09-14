package se.crisp.codekvast.agent.main;

import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;

import java.lang.annotation.*;

/**
 * Meta annotation for integration tests against an embedded CodekvastServerApplication
 *
 * @author olle.hallin@crisp.se
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringApplicationConfiguration(classes = CodekvastAgentApplication.class)
@IntegrationTest({
        "spring.profiles.active=httpPost",
        "spring.datasource.url=jdbc:h2:mem:integrationTest",
        "codekvast.apiAccessID=apiAccessID",
        "codekvast.apiAccessSecret=apiAccessSecret",
        "codekvast.serverUri=serverUri",
        "codekvast.dataPath=dataPath",
        "codekvast.serverUploadIntervalSeconds=600"
})
public @interface EmbeddedCodekvastAgentIntegTest {
}
