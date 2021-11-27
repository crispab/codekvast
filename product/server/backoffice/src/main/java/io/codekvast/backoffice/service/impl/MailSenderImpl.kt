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

import io.codekvast.backoffice.service.MailSender
import io.codekvast.common.logging.LoggerDelegate
import org.springframework.context.annotation.Profile
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

/** @author olle.hallin@crisp.se
 */
@Service
@Profile("!no-mail-sender")
class MailSenderImpl(
        private val javaMailSender: JavaMailSender,
        private val mailTemplateRenderer: MailTemplateRenderer) : MailSender {

    val logger by LoggerDelegate()

    override fun sendMail(template: MailSender.Template, emailAddress: String, vararg args: Any) {
        val body = mailTemplateRenderer.renderTemplate(template, *args)

        val mimeMessage = javaMailSender.createMimeMessage().apply {
            setHeader("Return-Path", "postmaster@codekvast.io")
        }

        MimeMessageHelper(mimeMessage, "UTF-8").apply {
            setSubject(template.subject)
            setFrom("no-reply@codekvast.io")
            setTo(emailAddress)
            setText(body, true)
        }

        try {
            javaMailSender.send(mimeMessage)
            logger.info("Sent mail with subject='{}' and body='{}' to {}", template.subject, body, emailAddress)
        } catch (e: MailSendException) {
            logger.warn(
                    "Failed to send mail with subject='{}' and body='{}' to {}: {}",
                    template.subject, body, emailAddress, e.toString())
            // Do not rethrow. Avoid spamming the log with stack traces.
        }
    }
}
