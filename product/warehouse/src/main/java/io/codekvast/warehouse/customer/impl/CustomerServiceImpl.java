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
package io.codekvast.warehouse.customer.impl;

import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.LicenseViolationException;
import io.codekvast.warehouse.customer.PricePlan;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private static final String SELECT_CLAUSE = "SELECT id, name, plan FROM customers ";

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public CustomerServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CustomerData getCustomerDataByLicenseKey(@NonNull String licenseKey) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("WHERE licenseKey = ?", licenseKey);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid license key: '" + licenseKey + "'");
        }
    }

    @Override
    public CustomerData getCustomerDataByCustomerId(long customerId) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("WHERE id = ?", customerId);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid customerId: " + customerId);
        }
    }

    @Override
    public CustomerData getCustomerDataByExternalId(@NonNull String externalId) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("WHERE externalId = ?", externalId);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid externalId: " + externalId);
        }
    }

    @Override
    public void assertPublicationSize(String licenseKey, int publicationSize) throws LicenseViolationException {
        doAssertNumberOfMethods(getCustomerDataByLicenseKey(licenseKey), publicationSize);
    }

    @Override
    public void assertDatabaseSize(long customerId) throws LicenseViolationException {
        CustomerData customerData = getCustomerDataByCustomerId(customerId);
        long numberOfMethods = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM methods WHERE customerId = ?", Long.class, customerId);

        doAssertNumberOfMethods(customerData, numberOfMethods);
    }

    @Override
    public Collection<CustomerData> getAllCustomers() {

        List<CustomerData> result = jdbcTemplate
            .query(SELECT_CLAUSE,
                   (rs, rowNum) -> CustomerData.builder()
                                               .customerId(rs.getLong(1))
                                               .customerName(rs.getString(2))
                                               .planName(rs.getString(3))
                                               .build());

        log.debug("Found {} customers", result.size());
        return result;
    }

    private CustomerData getCustomerData(String where_clause, Object identifier) {
        Map<String, Object> result = jdbcTemplate.queryForMap(SELECT_CLAUSE + where_clause, identifier);

        return CustomerData.builder()
                           .customerId((Long) result.get("id"))
                           .customerName((String) result.get("name"))
                           .planName((String) result.get("plan"))
                           .build();
    }

    private void doAssertNumberOfMethods(CustomerData customerData, long numberOfMethods) {
        log.debug("Asserting {} methods for {}", numberOfMethods, customerData);

        PricePlan pp = customerData.getPricePlan();
        if (numberOfMethods > pp.getMaxMethods()) {
            throw new LicenseViolationException(
                String.format("Too many methods: %d. The plan '%s' has a limit of %d methods",
                              numberOfMethods, customerData.getPlanName(), pp.getMaxMethods()));
        }
    }

}
