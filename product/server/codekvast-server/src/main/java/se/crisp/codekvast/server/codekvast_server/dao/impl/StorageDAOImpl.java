package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent.model.v1.*;
import se.crisp.codekvast.server.codekvast_server.dao.StorageDAO;
import se.crisp.codekvast.server.codekvast_server.exceptions.CodekvastException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedApplicationException;
import se.crisp.codekvast.server.codekvast_server.exceptions.UndefinedCustomerException;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
        long customerId = getOrCreateCustomer(data.getHeader().getCustomerName());
        long appId = getOrCreateApp(customerId, data.getHeader(), data.getAppName(), data.getAppVersion());
        storeJvmRunData(customerId, appId, data);
    }

    @Override
    @Transactional
    public Collection<UsageDataEntry> storeUsageData(UsageData usageData) throws CodekvastException {
        final Collection<UsageDataEntry> result = new ArrayList<>();

        long customerId = getOrCreateCustomer(usageData.getHeader().getCustomerName());
        long appId = getAppId(customerId, usageData.getJvmFingerprint());
        for (UsageDataEntry entry : usageData.getUsage()) {
            storeOrUpdateUsageDataEntry(result, customerId, appId, usageData.getJvmFingerprint(), entry);
        }
        return result;
    }

    private void storeOrUpdateUsageDataEntry(Collection<UsageDataEntry> result, long customerId, long appId, String jvmFingerprint,
                                             UsageDataEntry entry) {
        Date usedAt = entry.getUsedAtMillis() == null ? null : new Date(entry.getUsedAtMillis());
        Integer confidence = entry.getConfidence() == null ? null : entry.getConfidence().ordinal();

        int updated = attemptToUpdateSignature(customerId, appId, jvmFingerprint, entry, usedAt, confidence);

        if (updated > 0) {
            log.trace("Updated {}", entry);
            result.add(entry);
            return;
        }

        try {
            jdbcTemplate.update("INSERT INTO signatures(customer_id, application_id, signature, jvm_fingerprint, used_at, confidence) " +
                                        "VALUES(?, ?, ?, ?, ?, ?)", customerId, appId, entry.getSignature(), jvmFingerprint, usedAt,
                                confidence);
            log.trace("Stored {}", entry);
            result.add(entry);
        } catch (Exception ignore) {
            log.debug("Ignored attempt to insert duplicate signature");
        }
    }

    private int attemptToUpdateSignature(long customerId, long appId, String jvmFingerprint, UsageDataEntry entry, Date usedAt,
                                         Integer confidence) {
        if (usedAt == null) {
            // An unused signature is not allowed to overwrite a used signature
            return jdbcTemplate.update("UPDATE signatures SET confidence = ? " +
                                               "WHERE customer_id = ? AND application_id = ? AND signature = ? AND used_at IS NULL ",
                                       confidence, customerId, appId, entry.getSignature());
        }

        // A usage. Overwrite whatever was there.
        return jdbcTemplate.update("UPDATE signatures SET used_at = ?, jvm_fingerprint = ?, confidence = ? " +
                                           "WHERE customer_id = ? AND application_id = ? AND signature = ? ",
                                   usedAt, jvmFingerprint, confidence, customerId, appId, entry.getSignature());

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UsageDataEntry> getSignatures(String customerName) throws CodekvastException {
        // TODO: don't allow null customerName
        Object[] args = customerName == null ? new Object[0] : new Object[]{getOrCreateCustomer(customerName)};
        String where = customerName == null ? "" : " WHERE customer_id = ?";

        return jdbcTemplate.query("SELECT signature, used_at, confidence FROM signatures " + where,
                                  args, new UsageDataEntryRowMapper());
    }

    private void storeJvmRunData(long customerId, long appId, JvmRunData data) {
        Date dumpedAt = new Date(data.getDumpedAtMillis());

        int updated =
                jdbcTemplate.update("UPDATE jvm_runs SET dumped_at = ? WHERE jvm_fingerprint = ?", dumpedAt, data.getJvmFingerprint());
        if (updated > 0) {
            log.debug("Updated dumpedAt={} for JVM run {}", dumpedAt, data.getJvmFingerprint());
            return;
        }

        int inserted =
                jdbcTemplate
                        .update("INSERT INTO jvm_runs(customer_id, application_id, host_name, jvm_fingerprint, codekvast_version, " +
                                        "codekvast_vcs_id, started_at, dumped_at)" +
                                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                customerId, appId, data.getHostName(), data.getJvmFingerprint(),
                                data.getCodekvastVersion(), data.getCodekvastVcsId(), new Date(data.getStartedAtMillis()),
                                dumpedAt);
        if (inserted > 0) {
            log.debug("Stored new JVM run {}", data);
        } else {
            log.warn("Could not insert {}", data);
        }
    }

    private Long getOrCreateApp(long customerId, Header header, String appName,
                                String appVersion) throws UndefinedApplicationException {
        return getOrCreateApp(customerId, header, appName, appVersion, true);
    }

    private Long getOrCreateApp(long customerId, Header header, String appName, String appVersion,
                                boolean allowRecursion) throws UndefinedApplicationException {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM applications " +
                                                       "WHERE customer_id = ? AND environment = ? AND name = ? AND version = ? ",
                                               Long.class,
                                               customerId, header.getEnvironment(), appName, appVersion);
        } catch (EmptyResultDataAccessException ignored) {
        }
        if (!autoCreateApplication) {
            throw new UndefinedApplicationException("No such application: " + appName);
        }

        checkState(allowRecursion, "Endless recursion detected");

        int updated = jdbcTemplate.update("INSERT INTO applications(customer_id, environment, name, version) VALUES(?, ?, ?, ?)",
                                          customerId, header.getEnvironment(), appName, appVersion);
        if (updated > 0) {
            log.info("Created application {}:{}:{}:{}", header.getCustomerName(), header.getEnvironment(),
                     appName, appVersion);
            return getOrCreateApp(customerId, header, appName, appVersion, false);
        }

        throw new IllegalStateException("Could not insert application");
    }

    private long getAppId(long customerId, String jvmFingerprint) {
        return jdbcTemplate.queryForObject("SELECT application_id FROM jvm_runs " +
                                                   "WHERE customer_id = ? AND jvm_fingerprint = ?", Long.class,
                                           customerId, jvmFingerprint);
    }

    private long getOrCreateCustomer(final String customerName) throws UndefinedCustomerException {
        return getOrCreateCustomer(customerName, true);
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

    private static class UsageDataEntryRowMapper implements RowMapper<UsageDataEntry> {
        @Override
        public UsageDataEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UsageDataEntry(rs.getString(1), getTimeMillis(rs, 2),
                                      UsageConfidence.fromOrdinal(rs.getInt(3)));
        }

        private Long getTimeMillis(ResultSet rs, int columnIndex) throws SQLException {
            Date date = rs.getDate(columnIndex);
            return date == null ? null : date.getTime();
        }
    }
}
