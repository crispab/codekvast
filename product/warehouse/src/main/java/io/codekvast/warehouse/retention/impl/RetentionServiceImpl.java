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
package io.codekvast.warehouse.retention.impl;

import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.PricePlan;
import io.codekvast.warehouse.retention.RetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class RetentionServiceImpl implements RetentionService {

    private JdbcTemplate jdbcTemplate;
    private CustomerService customerService;

    @Inject
    public RetentionServiceImpl(JdbcTemplate jdbcTemplate, CustomerService customerService) {
        this.jdbcTemplate = jdbcTemplate;
        this.customerService = customerService;
    }

    @Override
    @Transactional
    public void performDataRetention() {
        Collection<CustomerData> customers = customerService.getAllCustomers();
        for (CustomerData customerData : customers) {
            PricePlan pp = customerData.getPricePlan();
            doCleanCustomer(customerData.getCustomerId(), pp.getRetentionDays());
        }
    }

    private void doCleanCustomer(long customerId, int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        long startedAt = System.currentTimeMillis();
        log.debug("Eliminating database rows older than {} for customer {}", cutoff, customerId);

        int deleted = jdbcTemplate.update("DELETE FROM invocations WHERE customerId = ? " +
                                              "AND timestamp < ? ",
                                          customerId, new Timestamp(cutoff.toEpochMilli()));
        log.debug("Deleted {} invocation rows", deleted);

        int sum = deleted;
        if (deleted > 0) {

            List<Integer> ids = jdbcTemplate.queryForList(
                "SELECT id FROM jvms j WHERE customerId = ? AND NOT EXISTS(SELECT i.customerId FROM invocations i WHERE i.jvmId = j.id)",
                Integer.class, customerId);

            if (!ids.isEmpty()) {
                jdbcTemplate.batchUpdate("DELETE FROM jvms WHERE id = ?", ids, 10, (ps, argument) -> ps.setLong(1, argument));

                log.debug("Deleted {} jvm rows", ids.size());
                sum += ids.size();
            }

            ids = jdbcTemplate.queryForList(
                "SELECT id FROM methods m WHERE customerId = ? AND NOT EXISTS(SELECT i.customerId FROM invocations i WHERE i.methodId = m" +
                    ".id)",
                Integer.class, customerId);

            if (!ids.isEmpty()) {
                jdbcTemplate.batchUpdate("DELETE FROM methods WHERE id = ?", ids, 10, (ps, argument) -> ps.setLong(1, argument));

                log.debug("Deleted {} method rows", ids.size());
                sum += ids.size();
            }

            ids = jdbcTemplate.queryForList(
                "SELECT id FROM applications a WHERE customerId = ? AND NOT EXISTS(SELECT i.customerId FROM invocations i WHERE i.applicationId = a.id)",
                Integer.class, customerId);

            if (!ids.isEmpty()) {
                jdbcTemplate.batchUpdate("DELETE FROM applications WHERE id = ?", ids, 10, (ps, argument) -> ps.setLong(1, argument));

                log.debug("Deleted {} application rows", ids.size());
                sum += ids.size();
            }
        }

        deleted = jdbcTemplate.update("DELETE FROM agent_state WHERE customerId = ? " +
                                          "AND timestamp < ? ",
                                      customerId, new Timestamp(cutoff.toEpochMilli()));
        log.debug("Deleted {} agent_state rows", deleted);
        sum += deleted;

        long elapsed = System.currentTimeMillis() - startedAt;
        if (sum == 0) {
            log.info("Nothing to delete for customer {}", customerId);
        } else {
            log.info("Deleted in total {} rows older than {} for customer {} in {} ms", sum, cutoff, customerId, elapsed);
        }
    }
}
