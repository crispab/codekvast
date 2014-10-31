package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exception.UndefinedCustomerException;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkState;

/**
 * DAO for user and application data.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class UserDAOImpl implements UserDAO {
    private final JdbcTemplate jdbcTemplate;
    private final Boolean autoCreateCustomer;
    private final Boolean autoCreateApplication;

    @Inject
    public UserDAOImpl(JdbcTemplate jdbcTemplate,
                       @Value("${codekvast.auto-register-customer}") Boolean autoCreateCustomer,
                       @Value("${codekvast.auto-register-application}") Boolean autoCreateApplication) {
        this.jdbcTemplate = jdbcTemplate;
        this.autoCreateCustomer = autoCreateCustomer;
        this.autoCreateApplication = autoCreateApplication;
    }

    private Long doGetOrCreateApp(long customerId, String environment, String appName, String appVersion, boolean allowRecursion)
            throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications " +
                                                       "WHERE customer_id = ? AND environment = ? AND name = ? AND version = ? ",
                                               Long.class,
                                               customerId, environment, appName, appVersion);
        } catch (EmptyResultDataAccessException ignored) {
        }

        if (!autoCreateApplication) {
            throw new UndefinedApplicationException("No such application: " + appName);
        }

        checkState(allowRecursion, "Endless recursion detected");

        int updated = jdbcTemplate.update("INSERT INTO applications(customer_id, environment, name, version) VALUES(?, ?, ?, ?)",
                                          customerId, environment, appName, appVersion);
        if (updated > 0) {
            log.info("Created application {}:{}:{}:{}", customerId, environment,
                     appName, appVersion);
            return doGetOrCreateApp(customerId, environment, appName, appVersion, false);
        }

        throw new IllegalStateException("Could not insert application");
    }

    @Override
    @Cacheable("user")
    public long getCustomerId(final String customerName) throws UndefinedCustomerException {
        log.debug("Looking up customer id for '{}'", customerName);
        return doGetOrCreateCustomer(customerName, true);
    }

    @Override
    @Cacheable("user")
    public long getAppId(long customerId, String environment, String appName, String appVersion) throws UndefinedApplicationException {
        log.debug("Looking up app id for {}:{}:{}", customerId, appName, appVersion);
        return doGetOrCreateApp(customerId, environment, appName, appVersion, true);
    }

    @Override
    @Cacheable("user")
    public AppId getAppId(String jvmFingerprint) {
        log.debug("Looking up AppId for JVM {}...", jvmFingerprint);

        String sql = "SELECT customer_id, application_id FROM jvm_runs WHERE jvm_fingerprint = ?";
        AppId result = jdbcTemplate.queryForObject(sql, new AppIdRowMapper(), jvmFingerprint);
        log.debug("Result = {}", result);
        return result;
    }

    private long doGetOrCreateCustomer(final String customerName, boolean allowRecursion) throws UndefinedCustomerException {
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
            return doGetOrCreateCustomer(customerName, false);
        }
        throw new IllegalStateException("Could not insert customer");
    }

    private static class AppIdRowMapper implements RowMapper<AppId> {
        @Override
        public AppId mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return new AppId(rs.getLong(1), rs.getLong(2));
        }
    }

}
