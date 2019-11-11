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

import com.google.common.annotations.VisibleForTesting;
import com.samskivert.mustache.Mustache;
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile("!no-mail-sender")
public class MailSenderImpl implements MailSender {

    private final JavaMailSender javaMailSender;
    private final Mustache.Compiler compiler;
    private final CustomerService customerService;

    @Override
    @SneakyThrows(MessagingException.class)
    public void sendMail(Template template, Long customerId, String emailAddress) {
        logger.info("Sending mail {} to {}", template, emailAddress);

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setSubject(template.getSubject());
        helper.setFrom("no-reply@codekvast.io");
        helper.setTo(emailAddress);
        helper.setText(renderTemplate(template, customerId), true);

        javaMailSender.send(mimeMessage);
    }

    @VisibleForTesting
    String renderTemplate(Template template, Long customerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerData", customerService.getCustomerDataByCustomerId(customerId));
        data.put("loginUrl", "https://login.codekvast.io");
        data.put("homepageUrl", "https://www.codekvast.io");
        data.put("supportEmail", "support@codekvast.io");

        return compiler.loadTemplate(getTemplateName(template)).execute(data);
    }

    private String getTemplateName(Template template) {
        return String.format("mail/%s", template.name().toLowerCase());
    }
}
