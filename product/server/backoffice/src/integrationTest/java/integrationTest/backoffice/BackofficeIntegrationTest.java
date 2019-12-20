package integrationTest.backoffice;

import io.codekvast.backoffice.CodekvastBackofficeApplication;
import io.codekvast.backoffice.service.MailSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.mail.MessagingException;

import static org.junit.Assume.assumeTrue;

/**
 * @author olle.hallin@crisp.se
 */
@SpringBootTest(
    classes = {CodekvastBackofficeApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"integrationTest", "dev-secrets", "no-mail-sender"})
@Transactional
public class BackofficeIntegrationTest {

    @Inject
    private MailSender mailSender;

    @Test
    public void should_send_mail() throws MessagingException {
        String recipient = System.getenv("RUN_MAIL_TESTS_SEND_TO");
        assumeTrue(recipient != null);
        mailSender.sendMail(MailSender.Template.WELCOME_TO_CODEKVAST, recipient, 1L);
    }
}
