package io.codekvast.backoffice.service.impl

import io.codekvast.backoffice.service.MailSender
import io.codekvast.backoffice.service.impl.MailTemplateRenderer.CodekvastFormatter
import io.codekvast.common.bootstrap.CodekvastCommonSettings
import io.codekvast.common.customer.CustomerData
import io.codekvast.common.customer.CustomerService
import io.codekvast.common.customer.PricePlan
import io.codekvast.common.customer.PricePlanDefaults
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream
import javax.inject.Inject
import kotlin.test.assertEquals

/** @author olle.hallin@crisp.se
 */
@SpringBootTest(classes = [CodekvastCommonSettings::class, MustacheAutoConfiguration::class, MailTemplateRenderer::class])
@SpringJUnitConfig
class MailTemplateRendererTest {
  @MockBean
  lateinit var customerService: CustomerService

  @Inject
  lateinit var settings: CodekvastCommonSettings

  @Inject
  lateinit var mailTemplateRenderer: MailTemplateRenderer

  @ParameterizedTest
  @MethodSource("formatterDataProvider")
  fun should_format_values_correctly(pair: Pair<Any, String>) {
    assertEquals(actual = CodekvastFormatter().format(pair.first), expected = pair.second)
  }

  @ParameterizedTest
  @MethodSource("welcomeDataProvider")
  @Throws(IOException::class)
  fun should_render_welcome_collection_has_started(customerData: CustomerData) {
    // given
    val displayVersion = "1.2.3-abcde"
    settings.displayVersion = displayVersion
    `when`(customerService.getCustomerDataByCustomerId(ArgumentMatchers.anyLong())).thenReturn(customerData)

    // when
    val message = mailTemplateRenderer.renderTemplate(MailSender.Template.WELCOME_TO_CODEKVAST, 1L).trim { it <= ' ' }

    // then
    verify(customerService).getCustomerDataByCustomerId(1L)
    MatcherAssert.assertThat(message, Matchers.containsString("Codekvast $displayVersion"))
    println("The rendered template = '$message'")
    val path = Files.createTempFile(javaClass.simpleName + "-", ".html")
    val writer = PrintWriter(FileWriter(path.toFile()))
    writer.println(message)
    writer.close()
    println("A copy of the rendered template is available in $path")
  }

  companion object {

    @Suppress("unused")
    @JvmStatic
    fun formatterDataProvider() = listOf(
      Pair(123, "123"),
      Pair(123456, "123,456"),
      Pair(123456L, "123,456"),
      Pair(123456.0, "123456.0"),
      Pair(123456f, "123456.0"),
      Pair(Instant.parse("2019-11-14T22:33:50.1234Z"), "2019-11-14 22:33:50 UTC"),
      Pair("foobar", "foobar"))

    @Suppress("unused")
    @JvmStatic
    fun welcomeDataProvider(): Stream<CustomerData> {
      val now = Instant.now()
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
          .build())
    }
  }
}
