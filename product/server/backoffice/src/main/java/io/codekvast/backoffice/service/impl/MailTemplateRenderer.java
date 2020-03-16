/*
 * Copyright (c) 2015-2020 Hallin Information Technology AB
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
import io.codekvast.backoffice.service.MailSender;
import io.codekvast.common.bootstrap.CodekvastCommonSettings;
import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import java.time.Instant;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** @author olle.hallin@crisp.se */
@Component
@RequiredArgsConstructor
public class MailTemplateRenderer {
  private final Mustache.Compiler compiler;
  private final CustomerService customerService;
  private final CodekvastCommonSettings commonSettings;

  String renderTemplate(MailSender.Template template, Object... args) {
    Map<String, Object> data = collectCommonData();

    //noinspection SwitchStatementWithTooFewBranches
    switch (template) {
      case WELCOME_TO_CODEKVAST:
        Long customerId = (Long) args[0];
        collectWelcomeToCodekvastData(customerId, data);
        break;
      default:
        throw new IllegalArgumentException("Don't know how to render " + template);
    }

    return compiler
        .withFormatter(new CodekvastFormatter())
        .loadTemplate(getTemplateName(template))
        .execute(data);
  }

  private Map<String, Object> collectCommonData() {
    Map<String, Object> data = new HashMap<>();
    data.put("codekvastDisplayVersion", commonSettings.getDisplayVersion());
    data.put("homepageUrl", commonSettings.getHomepageBaseUrl());
    data.put("loginUrl", commonSettings.getLoginBaseUrl());
    data.put("supportEmail", commonSettings.getSupportEmail());
    return data;
  }

  private void collectWelcomeToCodekvastData(Long customerId, Map<String, Object> data) {
    CustomerData customerData = customerService.getCustomerDataByCustomerId(customerId);
    data.put("customerName", customerData.getDisplayName());
    data.put("pricePlan", customerData.getPricePlan());
    data.put(
        "inTrialPeriod",
        customerData.getCollectionStartedAt() != null
            && customerData.getTrialPeriodEndsAt() != null);
    data.put("hasTrialPeriodDays", customerData.getPricePlan().getTrialPeriodDays() > 0);
    data.put("trialPeriodEndsAt", customerData.getTrialPeriodEndsAt());
    data.put("trialPeriodStartedAt", customerData.getCollectionStartedAt());
  }

  private String getTemplateName(MailSender.Template template) {
    return String.format("mail/%s", template.name().toLowerCase());
  }

  static class CodekvastFormatter implements Mustache.Formatter {
    @SuppressWarnings("ChainOfInstanceofChecks")
    @Override
    public String format(Object value) {
      if (value instanceof Instant) {
        return value
            .toString()
            .replace("T", " ") // the 'T' between date and time
            .replaceAll("\\.[0-9]+Z$", " UTC"); // the milliseconds and timezone part
      }
      if (value instanceof Integer) {
        return new Formatter(Locale.ENGLISH).format("%,d", value).toString();
      }
      if (value instanceof Long) {
        return new Formatter(Locale.ENGLISH).format("%,d", value).toString();
      }
      return String.valueOf(value);
    }
  }
}
