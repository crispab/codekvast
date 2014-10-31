package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent.model.v1.JvmRunData;
import se.crisp.codekvast.server.agent.model.v1.UsageConfidence;
import se.crisp.codekvast.server.agent.model.v1.UsageData;
import se.crisp.codekvast.server.agent.model.v1.UsageDataEntry;
import se.crisp.codekvast.server.codekvast_server.dao.UsageDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * DAO for signature data.
 *
 * @author Olle Hallin
 */
@Repository
@Slf4j
public class UsageDAOImpl implements UsageDAO {

    private final JdbcTemplate jdbcTemplate;
    private final UserDAO userDAO;

    @Inject
    public UsageDAOImpl(JdbcTemplate jdbcTemplate, UserDAO userDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDAO = userDAO;
    }

    @Override
    @Transactional
    public void storeJvmRunData(JvmRunData data) throws CodekvastException {
        long customerId = userDAO.getCustomerId(data.getHeader().getCustomerName());
        long appId = userDAO.getAppId(customerId, data.getHeader().getEnvironment(), data.getAppName(), data.getAppVersion());
        storeJvmRunData(customerId, appId, data);
    }

    @Override
    @Transactional
    public Collection<UsageDataEntry> storeUsageData(UsageData usageData) throws CodekvastException {
        final Collection<UsageDataEntry> result = new ArrayList<>();

        UserDAO.AppId appId = userDAO.getAppId(usageData.getJvmFingerprint());

        for (UsageDataEntry entry : usageData.getUsage()) {
            storeOrUpdateUsageDataEntry(result, appId, usageData.getJvmFingerprint(), entry);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UsageDataEntry> getSignatures(String customerName) throws CodekvastException {
        // TODO: don't allow null customerName
        Object[] args = customerName == null ? new Object[0] : new Object[]{userDAO.getCustomerId(customerName)};
        String where = customerName == null ? "" : " WHERE customer_id = ?";

        return jdbcTemplate.query("SELECT signature, used_at, confidence FROM signatures " + where,
                                  args, new UsageDataEntryRowMapper());
    }

    private void storeOrUpdateUsageDataEntry(Collection<UsageDataEntry> result, UserDAOImpl.AppId appId, String jvmFingerprint,
                                             UsageDataEntry entry) {
        Date usedAt = entry.getUsedAtMillis() == null ? null : new Date(entry.getUsedAtMillis());
        Integer confidence = entry.getConfidence() == null ? null : entry.getConfidence().ordinal();

        int updated = attemptToUpdateSignature(appId, jvmFingerprint, entry, usedAt, confidence);

        if (updated > 0) {
            log.trace("Updated {}", entry);
            result.add(entry);
            return;
        }

        try {
            jdbcTemplate.update("INSERT INTO signatures(customer_id, application_id, signature, jvm_fingerprint, used_at, confidence) " +
                                        "VALUES(?, ?, ?, ?, ?, ?)",
                                appId.getCustomerId(), appId.getAppId(), entry.getSignature(), jvmFingerprint, usedAt, confidence);
            log.trace("Stored {}", entry);
            result.add(entry);
        } catch (Exception ignore) {
            log.trace("Ignored attempt to insert duplicate signature");
        }
    }

    private int attemptToUpdateSignature(UserDAOImpl.AppId appId, String jvmFingerprint, UsageDataEntry entry, Date usedAt,
                                         Integer confidence) {
        if (usedAt == null) {
            // An unused signature is not allowed to overwrite a used signature
            return jdbcTemplate.update("UPDATE signatures SET confidence = ? " +
                                               "WHERE customer_id = ? AND application_id = ? AND signature = ? AND used_at IS NULL ",
                                       confidence, appId.getCustomerId(), appId.getAppId(), entry.getSignature());
        }

        // A usage. Overwrite whatever was there.
        return jdbcTemplate.update("UPDATE signatures SET used_at = ?, jvm_fingerprint = ?, confidence = ? " +
                                           "WHERE customer_id = ? AND application_id = ? AND signature = ? ",
                                   usedAt, jvmFingerprint, confidence, appId.getCustomerId(), appId.getAppId(), entry.getSignature());

    }

    private void storeJvmRunData(long customerId, long appId, JvmRunData data) {
        Date dumpedAt = new Date(data.getDumpedAtMillis());

        int updated =
                jdbcTemplate
                        .update("UPDATE jvm_runs SET dumped_at = ? WHERE customer_id = ? AND application_id = ? AND jvm_fingerprint = ?",
                                dumpedAt, customerId, appId, data.getJvmFingerprint());
        if (updated > 0) {
            log.debug("Updated dumped_at={} for JVM run {}", dumpedAt, data.getJvmFingerprint());
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

    private static class UsageDataEntryRowMapper implements RowMapper<UsageDataEntry> {
        public static final Long EPOCH = 0L;

        @Override
        public UsageDataEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UsageDataEntry(rs.getString(1), getTimeMillis(rs, 2), UsageConfidence.fromOrdinal(rs.getInt(3)));
        }

        private Long getTimeMillis(ResultSet rs, int columnIndex) throws SQLException {
            Date date = rs.getTimestamp(columnIndex);
            return date == null ? EPOCH : Long.valueOf(date.getTime());
        }
    }

}
