package io.codekvast.backoffice.service.impl;

import static io.codekvast.backoffice.service.MailSender.Template.WELCOME_TO_CODEKVAST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.codekvast.backoffice.bootstrap.CodekvastBackofficeSettings;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.PricePlan;
import io.codekvast.common.customer.PricePlanDefaults;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** @author olle.hallin@crisp.se */
@SpringBootTest(
    classes = {
      CodekvastBackofficeSettings.class,
      MustacheAutoConfiguration.class,
      MailTemplateRenderer.class
    })
@SpringJUnitConfig
public class MailTemplateRendererTest {

  @MockBean private CustomerService customerService;

  @Inject private CodekvastBackofficeSettings settings;

  @Inject private MailTemplateRenderer mailTemplateRenderer;

  @ParameterizedTest
  @MethodSource("formatterDataProvider")
  void should_format_values_correctly(Object value, String expected) {
    assertThat(new MailTemplateRenderer.CodekvastFormatter().format(value), is(expected));
  }

  @ParameterizedTest
  @MethodSource("welcomeDataProvider")
  void should_render_welcome_collection_has_started(CustomerData customerData) throws IOException {
    // given
    String displayVersion = "1.2.3-abcde";
    settings.setDisplayVersion(displayVersion);
    when(customerService.getCustomerDataByCustomerId(anyLong())).thenReturn(customerData);

    // when
    String message = mailTemplateRenderer.renderTemplate(WELCOME_TO_CODEKVAST, 1L).trim();

    // then
    verify(customerService).getCustomerDataByCustomerId(1L);
    assertThat(message, containsString("Codekvast " + displayVersion));

    System.out.println("The rendered template = '" + message + "'");

    Path path = Files.createTempFile(getClass().getSimpleName() + "-", ".html");
    PrintWriter writer = new PrintWriter(new FileWriter(path.toFile()));
    writer.println(message);
    writer.close();
    System.out.println("A copy of the rendered template is available in " + path);
  }

  static Object[][] formatterDataProvider() {
    return new Object[][] {
      {123, "123"},
      {123456, "123,456"},
      {123456L, "123,456"},
      {123456D, "123456.0"},
      {123456F, "123456.0"},
      {Instant.parse("2019-11-14T22:33:50.1234Z"), "2019-11-14 22:33:50 UTC"},
      {"foobar", "foobar"}
    };
  }

  static Stream<CustomerData> welcomeDataProvider() {
    Instant now = Instant.now();
    return Stream.of(
        CustomerData.sample(),
        CustomerData.sample()
            .toBuilder()
            .collectionStartedAt(now)
            .trialPeriodEndsAt(now.plus(14, ChronoUnit.DAYS))
            .build(),
        CustomerData.sample()
            .toBuilder()
            .pricePlan(PricePlan.of(PricePlanDefaults.TEST).toBuilder().trialPeriodDays(-1).build())
            .build());
  }
}
