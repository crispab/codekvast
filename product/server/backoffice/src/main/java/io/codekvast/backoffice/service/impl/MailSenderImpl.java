/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.backoffice.service.impl;

import io.codekvast.backoffice.service.MailSender;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("!no-mail-sender")
public class MailSenderImpl implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailTemplateRenderer mailTemplateRenderer;

    @Override
    @SneakyThrows(MessagingException.class)
    public void sendMail(Template template, String emailAddress, Object... args) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        mimeMessage.setHeader("Return-Path", "postmaster@codekvast.io");

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setSubject(template.getSubject());
        helper.setFrom("no-reply@codekvast.io");
        helper.setTo(emailAddress);
        String body = mailTemplateRenderer.renderTemplate(template, args);
        helper.setText(body, true);

        try {
            javaMailSender.send(mimeMessage);
            logger.info("Sent mail with subject='{}' and body='{}' to {}", template.getSubject(), body, emailAddress);
        } catch (MailSendException e) {
            logger.warn("Failed to send mail with subject='{}' and body='{}' to {}: {}", template.getSubject(), body, emailAddress, e.toString());
            // Do not rethrow. Avoid spamming the log with stack traces.
        }
    }

}
