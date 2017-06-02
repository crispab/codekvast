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
package io.codekvast.warehouse.heroku.impl;

import io.codekvast.warehouse.bootstrap.CodekvastSettings;
import io.codekvast.warehouse.heroku.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class HerokuServiceImpl implements HerokuService {

    private final CodekvastSettings settings;
    private final JdbcTemplate jdbcTemplate;

    @Inject
    public HerokuServiceImpl(CodekvastSettings settings, JdbcTemplate jdbcTemplate) throws NoSuchAlgorithmException {
        this.settings = settings;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public HerokuProvisionResponse provision(HerokuProvisionRequest request) {
        log.debug("Handling {}", request);

        String licenseKey = UUID.randomUUID().toString().replaceAll("[-_]", "").toUpperCase();

        jdbcTemplate.update("INSERT INTO customers(externalId, name, licenseKey, plan) VALUES(?, ?, ?, ?)",
                            request.getUuid(), request.getHeroku_id(), licenseKey, request.getPlan());

        Map<String, String> config = new HashMap<>();
        config.put("CODEKVAST_URL", settings.getHerokuCodekvastUrl());
        config.put("CODEKVAST_LICENSE_KEY", licenseKey);

        HerokuProvisionResponse response = HerokuProvisionResponse.builder()
                                                                  .id(request.getUuid())
                                                                  .config(config)
                                                                  .build();
        log.debug("Returning {}", response);
        return response;
    }

    @Override
    @Transactional
    public void changePlan(String externalId, HerokuChangePlanRequest request) {
        log.debug("Received {} for customers.externalId={}", request, externalId);

        Map<String, Object> customer;
        try {
            customer = jdbcTemplate.queryForMap("SELECT name, plan FROM customers WHERE externalId = ?", externalId);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.warn("Invalid customer.externalId: {}", externalId);
            return;
        }

        String name = (String) customer.get("name");
        String oldPlan = (String) customer.get("plan");

        if (request.getPlan().equals(oldPlan)) {
            log.info("'{}' is already on plan '{}'", name, request.getPlan());
            return;
        }

        int count = jdbcTemplate.update("UPDATE customers SET plan = ? WHERE externalId = ?",
                                        request.getPlan(), externalId);

        if (count == 0) {
            log.warn("Failed to change plan for '{}' from '{}' to '{}'", name, oldPlan, request.getPlan());
        } else {
            log.info("Changed plan for '{}' from '{}' to '{}'", name, oldPlan, request.getPlan());
            // TODO: adjust to new plan
        }
    }

    @Override
    @Transactional
    public void deprovision(String externalId) {
        Long customerId;
        try {
            customerId = jdbcTemplate.queryForObject("SELECT id FROM customers WHERE externalId = ?", Long.class, externalId);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.warn("Invalid customer.externalId: {}", externalId);
            return;
        }

        int count = jdbcTemplate.update("DELETE FROM invocations WHERE customerId = ?", customerId);
        log.debug("Deleted {} invocation rows", count);

        count = jdbcTemplate.update("DELETE FROM methods WHERE customerId = ?", customerId);
        log.debug("Deleted {} method rows", count);

        count = jdbcTemplate.update("DELETE FROM jvms WHERE customerId = ?", customerId);
        log.debug("Deleted {} method rows", count);

        count = jdbcTemplate.update("DELETE FROM applications WHERE customerId = ?", customerId);
        log.debug("Deleted {} application rows", count);

        count = jdbcTemplate.update("DELETE FROM customers WHERE id = ?", customerId);
        log.debug("Deleted {} customer rows", count);
    }

}
