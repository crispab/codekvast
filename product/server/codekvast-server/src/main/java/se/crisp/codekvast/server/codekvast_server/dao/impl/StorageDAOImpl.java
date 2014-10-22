package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent.model.v1.Header;
import se.crisp.codekvast.server.codekvast_server.dao.StorageDAO;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedCustomerException;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;

/**
 * DAO for customer data.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class StorageDAOImpl implements StorageDAO {

    private final JdbcTemplate jdbcTemplate;
    private final Boolean autoCreateCustomer;
    private final Boolean autoCreateApplication;

    @Inject
    public StorageDAOImpl(JdbcTemplate jdbcTemplate,
                          @Value("${codekvast.auto-register-customer}") Boolean autoCreateCustomer,
                          @Value("${codekvast.auto-register-application}") Boolean autoCreateApplication) {
        this.jdbcTemplate = jdbcTemplate;
        this.autoCreateCustomer = autoCreateCustomer;
        this.autoCreateApplication = autoCreateApplication;
    }

    @Override
    @Transactional
    public void storeApplicationData(Header header) throws CodekvastException {
        long customerId = getOrCreateCustomer(header.getCustomerName(), true);
        createAppIfMissing(customerId, header, true);
    }

    private Long createAppIfMissing(long customerId, Header header, boolean allowRecursion) throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications " +
                                                       "WHERE customer_id = ? AND name = ? AND version = ? AND environment = ? ",
                                               Long.class,
                                               customerId, header.getAppName(), header.getAppVersion(), header.getEnvironment());
        } catch (EmptyResultDataAccessException ignored) {
        }
        if (!autoCreateApplication) {
            throw new UndefinedApplicationException("No such application: " + header.getAppName());
        }

        checkState(allowRecursion, "Endless recursion not allowed");

        int updated = jdbcTemplate.update("INSERT INTO applications(customer_id, name, version, environment) VALUES(?, ?, ?, ?)",
                                          customerId, header.getAppName(), header.getAppVersion(), header.getEnvironment());
        if (updated > 0) {
            log.info("Created application {}:{}:{}:{}", header.getCustomerName(), header.getAppName(),
                     header.getAppVersion(), header.getEnvironment());
            return createAppIfMissing(customerId, header, false);
        }

        throw new IllegalStateException("Could not insert application");
    }

    private long getOrCreateCustomer(final String customerName, boolean allowRecursion) throws UndefinedCustomerException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM customers WHERE name = ?", Long.class, customerName);
        } catch (EmptyResultDataAccessException ignored) {
        }
        if (!autoCreateCustomer) {
            throw new UndefinedCustomerException("No such customer: " + customerName);
        }

        checkState(allowRecursion, "Endless recursion not allowed");

        int updated = jdbcTemplate.update("INSERT INTO customers(name) VALUES(?)", customerName);
        if (updated > 0) {
            log.info("Created customer '{}'", customerName);
            return getOrCreateCustomer(customerName, false);
        }
        throw new IllegalStateException("Could not insert customer");
    }
}
