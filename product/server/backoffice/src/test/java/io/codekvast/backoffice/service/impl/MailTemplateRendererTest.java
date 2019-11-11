package io.codekvast.backoffice.service.impl;

import io.codekvast.backoffice.bootstrap.CodekvastBackofficeSettings;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import static io.codekvast.backoffice.service.MailSender.Template.WELCOME_COLLECTION_HAS_STARTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author olle.hallin@crisp.se
 */
@SpringBootTest(classes={CodekvastBackofficeSettings.class, MustacheAutoConfiguration.class, MailTemplateRenderer.class})
@RunWith(SpringRunner.class)
class MailTemplateRendererTest {

    @MockBean
    private CustomerService customerService;

    @Inject
    private CodekvastBackofficeSettings settings;

    @Inject
    private MailTemplateRenderer mailTemplateRenderer;

    @Test
    void should_render_template() {
        // given
        String displayVersion = "1.2.3-abcde";
        settings.setDisplayVersion(displayVersion);
        when(customerService.getCustomerDataByCustomerId(anyLong())).thenReturn(CustomerData.sample());

        // when
        String message = mailTemplateRenderer.renderTemplate(WELCOME_COLLECTION_HAS_STARTED, 1L).trim();

        // then
        verify(customerService).getCustomerDataByCustomerId(1L);
        assertThat(message, containsString("Codekvast " + displayVersion));
    }
}
