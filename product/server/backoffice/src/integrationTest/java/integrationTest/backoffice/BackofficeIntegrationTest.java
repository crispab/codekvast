package integrationTest.backoffice;

import io.codekvast.backoffice.CodekvastBackofficeApplication;
import io.codekvast.backoffice.service.MailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@SpringBootTest(
    classes = {CodekvastBackofficeApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"integrationTest", "dev-secrets"})
@Transactional
public class BackofficeIntegrationTest {

    @Inject
    private MailSender mailSender;

    @Test
    public void should_send_mail() {
        mailSender.sendMail(MailSender.Template.WELCOME_COLLECTION_HAS_STARTED, 1L, "olle.hallin@gmail.com");
    }
}
