package io.codekvast.backoffice.service.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static io.codekvast.backoffice.service.MailSender.Template.WELCOME_COLLECTION_HAS_STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@SpringBootTest(classes={MailSenderImpl.class, MustacheAutoConfiguration.class})
@RunWith(SpringRunner.class)
class MailSenderImplTest {

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private CustomerService customerService;

    @Inject
    private MailSenderImpl mailSender;

    @MockBean
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void should_render_template() throws MessagingException {
        // given
        when(customerService.getCustomerDataByCustomerId(anyLong())).thenReturn(CustomerData.sample());

        // when
        String message = mailSender.renderTemplate(WELCOME_COLLECTION_HAS_STARTED, 1L).trim();

        // then
        verify(customerService).getCustomerDataByCustomerId(1L);
        assertThat(message, startsWith("<html>"));
        assertThat(message, endsWith("</html>"));
    }
}
