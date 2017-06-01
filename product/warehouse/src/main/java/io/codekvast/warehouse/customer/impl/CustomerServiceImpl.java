package io.codekvast.warehouse.customer.impl;

import io.codekvast.warehouse.customer.CustomerData;
import io.codekvast.warehouse.customer.CustomerService;
import io.codekvast.warehouse.customer.LicenseViolationException;
import io.codekvast.warehouse.customer.PricePlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

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
    public CustomerData getCustomerDataByLicenseKey(String licenseKey) throws LicenseViolationException {
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT id, plan FROM customers WHERE licenseKey = ?", licenseKey.trim());

            return CustomerData.builder()
                               .customerId((Long) result.get("id"))
                               .planName((String) result.get("plan"))
                               .build();

        } catch (DataAccessException e) {
            throw new LicenseViolationException("Invalid license key: '" + licenseKey + "'");
        }
    }

    @Override
    public CustomerData getCustomerDataByCustomerId(long customerId) throws LicenseViolationException {
        try {
            String planName = jdbcTemplate.queryForObject("SELECT plan FROM customers WHERE id = ?", String.class, customerId);

            return CustomerData.builder()
                               .customerId(customerId)
                               .planName(planName)
                               .build();

        } catch (DataAccessException e) {
            throw new LicenseViolationException("Invalid customerId: " + customerId);
        }
    }

    @Override
    public void checkLicenseKey(String licenseKey) throws LicenseViolationException {
        getCustomerDataByLicenseKey(licenseKey);
    }

    @Override
    public void assertDatabaseSize(long customerId) throws LicenseViolationException {
        CustomerData customerData = getCustomerDataByCustomerId(customerId);
        long numberOfMethods = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM methods WHERE customerId = ?", Long.class, customerId);

        log.debug("Customer {} has {} methods in plan '{}'", customerId, numberOfMethods, customerData.getPlanName());

        PricePlan pp = customerData.getPricePlan();
        if (numberOfMethods > pp.getMaxMethods()) {

            throw new LicenseViolationException(
                String.format("Too many methods: %d. The plan '%s' has a limit of %d methods",
                              numberOfMethods, customerData.getPlanName(), pp.getMaxMethods()));
        }
    }

}
