package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent.model.v1.Header;
import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.codekvast_server.dao.StorageDAO;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedCustomerException;

import javax.inject.Inject;
import java.util.Date;

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
    public void storeJvmRunData(JvmRunData data) throws CodekvastException {
        long customerId = getOrCreateCustomer(data.getHeader().getCustomerName(), true);
        long appId = getOrCreateApp(customerId, data.getHeader(), true);
        storeJvmRunData(customerId, appId, data);
    }

    private void storeJvmRunData(long customerId, long appId, JvmRunData data) {
        String uuid = data.getUuid().toString();
        Date dumpedAt = new Date(data.getDumpedAtMillis());

        int updated = jdbcTemplate.update("UPDATE jvm_runs SET dumped_at = ? WHERE uuid = ?", dumpedAt, uuid);
        if (updated > 0) {
            log.debug("Updated dumpedAt={} for JVM run {}", dumpedAt, uuid);
            return;
        }

        int inserted = jdbcTemplate.update("INSERT INTO jvm_runs(customer_id, application_id, host_name, uuid, started_at, dumped_at)" +
                                                   " VALUES (?, ?, ?, ?, ?, ?)",
                                           customerId, appId, data.getHostName(), uuid, new Date(data.getStartedAtMillis()), dumpedAt);
        if (inserted > 0) {
            log.debug("Stored new JVM run {}", data);
        } else {
            log.warn("Could not insert {}", data);
        }
    }

    private Long getOrCreateApp(long customerId, Header header, boolean allowRecursion) throws UndefinedApplicationException {
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
            return getOrCreateApp(customerId, header, false);
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
