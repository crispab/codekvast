/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
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
package io.codekvast.login.heroku.impl;

import io.codekvast.common.customer.CustomerService;
import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.heroku.HerokuException;
import io.codekvast.login.heroku.HerokuService;
import io.codekvast.login.heroku.model.HerokuChangePlanRequest;
import io.codekvast.login.heroku.model.HerokuProvisionRequest;
import io.codekvast.login.heroku.model.HerokuProvisionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HerokuServiceImpl implements HerokuService {

    private final CodekvastLoginSettings settings;
    private final CustomerService customerService;

    @Override
    public HerokuProvisionResponse provision(HerokuProvisionRequest request) throws HerokuException {
        logger.debug("Handling {}", request);
        try {
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
        } catch (Exception e) {
            throw new HerokuException("Could not execute " + request, e);
        }
    }

    @Override
    public void changePlan(String externalId, HerokuChangePlanRequest request) throws HerokuException {
        logger.debug("Received {} for customers.externalId={}", request, externalId);
        try {
            customerService.changePlanForExternalId(externalId, request.getPlan());
        } catch (Exception e) {
            throw new HerokuException("Could not execute " + request + " for externalId '" + externalId + "'", e);
        }
    }

    @Override
    public void deprovision(String externalId) throws HerokuException {
        try {
            customerService.deleteCustomerByExternalId(externalId);
        } catch (Exception e) {
            throw new HerokuException("Could not deprovision externalId '" + externalId + "'", e);
        }
    }

}
