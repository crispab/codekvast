/*
 * Copyright (c) 2015-2021 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.backoffice.service.impl

import com.samskivert.mustache.Mustache
import io.codekvast.backoffice.service.MailSender
import io.codekvast.backoffice.service.MailSender.Template.WELCOME_TO_CODEKVAST
import io.codekvast.common.bootstrap.CodekvastCommonSettings
import io.codekvast.common.customer.CustomerService
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/** @author olle.hallin@crisp.se
 */
@Component
class MailTemplateRenderer(
        private val compiler: Mustache.Compiler,
        private val customerService: CustomerService,
        private val commonSettings: CodekvastCommonSettings) {

    fun renderTemplate(template: MailSender.Template, vararg args: Any): String {
        val templateSpecificData: Map<String, Any>

        when (template) {
            WELCOME_TO_CODEKVAST -> templateSpecificData = collectWelcomeToCodekvastData(args[0] as Long)
        }

        return compiler
                .withFormatter(CodekvastFormatter())
                .loadTemplate(getTemplateName(template))
                .execute(collectCommonData() + templateSpecificData)
    }

    private fun collectCommonData() = mutableMapOf(
            "codekvastDisplayVersion" to commonSettings.displayVersion,
            "homepageUrl" to commonSettings.homepageBaseUrl,
            "loginUrl" to commonSettings.loginBaseUrl,
            "supportEmail" to commonSettings.supportEmail)

    private fun collectWelcomeToCodekvastData(customerId: Long): Map<String, Any> {
        val customerData = customerService.getCustomerDataByCustomerId(customerId)
        return mapOf(
                "customerName" to customerData.displayName,
                "pricePlan" to customerData.pricePlan,
                "inTrialPeriod" to (customerData.collectionStartedAt != null && customerData.trialPeriodEndsAt != null),
                "hasTrialPeriodDays" to (customerData.pricePlan.trialPeriodDays > 0),
                "trialPeriodEndsAt" to customerData.trialPeriodEndsAt,
                "trialPeriodStartedAt" to customerData.collectionStartedAt
        )
    }

    private fun getTemplateName(template: MailSender.Template): String {
        return String.format("mail/%s", template.name.lowercase())
    }

    class CodekvastFormatter : Mustache.Formatter {

        override fun format(value: Any) = when (value) {
            is Instant -> value.toString()
                    .replace("T", " ") // the 'T' between date and time
                    .replace("\\.[0-9]+Z$".toRegex(), " UTC")

            is Int, is Long -> Formatter(Locale.ENGLISH).format("%,d", value).toString()

            else -> value.toString()
        }
    }

}
