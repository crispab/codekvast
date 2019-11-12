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

import com.samskivert.mustache.Mustache;
import io.codekvast.backoffice.bootstrap.CodekvastBackofficeSettings;
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@RequiredArgsConstructor
public class MailTemplateRenderer {
    private final Mustache.Compiler compiler;
    private final CustomerService customerService;
    private final CodekvastBackofficeSettings settings;

    String renderTemplate(MailSender.Template template, Object... args) {
        Map<String, Object> data = collectCommonData();

        //noinspection SwitchStatementWithTooFewBranches
        switch (template) {
        case WELCOME_COLLECTION_HAS_STARTED:
            Long customerId = (Long) args[0];
            collectWelcomeCollectionHasStartedData(data, customerId);
            break;
        }

        return compiler.withFormatter(new CodekvastFormatter()).loadTemplate(getTemplateName(template)).execute(data);
    }

    private void collectWelcomeCollectionHasStartedData(Map<String, Object> data, Long customerId) {
        CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);
        data.put("customerName", customerData.getDisplayName());
        data.put("pricePlan", customerData.getPricePlan());
        data.put("inTrialPeriod", customerData.getCollectionStartedAt() != null && customerData.getTrialPeriodEndsAt() != null);
        data.put("trialPeriodEndsAt", customerData.getTrialPeriodEndsAt());
        data.put("trialPeriodStartedAt", customerData.getCollectionStartedAt());
    }

    private Map<String, Object> collectCommonData() {
        Map<String, Object> data = new HashMap<>();
        data.put("codekvastDisplayVersion", settings.getDisplayVersion());
        data.put("homepageUrl", settings.getHomepageBaseUrl());
        data.put("loginUrl", settings.getLoginBaseUrl());
        data.put("supportEmail", settings.getSupportEmail());
        return data;
    }

    private String getTemplateName(MailSender.Template template) {
        return String.format("mail/%s", template.name().toLowerCase());
    }

    private class CodekvastFormatter implements Mustache.Formatter {
        @SuppressWarnings("ChainOfInstanceofChecks")
        @Override
        public String format(Object value) {
            if (value instanceof Instant) {
                return value.toString().replace("T", " ").replaceAll("\\.[0-9]+", "").replaceAll("Z$", " UTC");
            }
            if (value instanceof Integer) {
                return String.format("%,d", value);
            }
            return String.valueOf(value);
        }
    }
}
