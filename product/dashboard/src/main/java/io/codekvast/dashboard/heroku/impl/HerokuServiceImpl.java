/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.heroku.impl;

import io.codekvast.dashboard.bootstrap.CodekvastSettings;
import io.codekvast.dashboard.customer.CustomerService;
import io.codekvast.dashboard.heroku.HerokuChangePlanRequest;
import io.codekvast.dashboard.heroku.HerokuProvisionRequest;
import io.codekvast.dashboard.heroku.HerokuProvisionResponse;
import io.codekvast.dashboard.heroku.HerokuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class HerokuServiceImpl implements HerokuService {

    private final CodekvastSettings settings;
    private final CustomerService customerService;

    @Inject
    public HerokuServiceImpl(CodekvastSettings settings, CustomerService customerService) throws NoSuchAlgorithmException {
        this.settings = settings;
        this.customerService = customerService;
    }

    @Override
    public HerokuProvisionResponse provision(HerokuProvisionRequest request) {
        logger.debug("Handling {}", request);

        String licenseKey = customerService.addCustomer(CustomerService.AddCustomerRequest
                                                            .builder()
                                                            .source(CustomerService.Source.HEROKU)
                                                            .externalId(request.getUuid())
                                                            .name(request.getHeroku_id())
                                                            .plan(request.getPlan())
                                                            .build());

        Map<String, String> config = new HashMap<>();
        config.put("CODEKVAST_URL", settings.getHerokuCodekvastUrl());
        config.put("CODEKVAST_LICENSE_KEY", licenseKey);

        HerokuProvisionResponse response = HerokuProvisionResponse.builder()
                                                                  .id(request.getUuid())
                                                                  .config(config)
                                                                  .build();
        logger.debug("Returning {}", response);
        return response;
    }

    @Override
    public void changePlan(String externalId, HerokuChangePlanRequest request) {
        logger.debug("Received {} for customers.externalId={}", request, externalId);

        customerService.changePlanForExternalId(externalId, request.getPlan());
    }

    @Override
    public void deprovision(String externalId) {
        customerService.deleteCustomerByExternalId(externalId);
    }

}
