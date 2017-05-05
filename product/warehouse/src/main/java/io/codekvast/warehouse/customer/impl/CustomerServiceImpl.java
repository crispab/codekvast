/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.customer.impl;

import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.LicenseViolationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public CustomerServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long checkLicenseKeyAndGetCustomerId(@NonNull String licenseKey) throws LicenseViolationException {
        List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM customers WHERE licenseKey = ?", Long.class, licenseKey.trim());

        if (ids.isEmpty()) {
            throw new LicenseViolationException("Invalid license key: '" + licenseKey + "'");
        }

        long result = ids.get(0);
        log.debug("licenseKey '{}' belongs to customer {}", licenseKey, result);
        return result;
    }
}
