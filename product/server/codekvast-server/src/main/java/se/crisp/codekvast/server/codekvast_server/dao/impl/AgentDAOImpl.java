package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent.model.v1.InvocationData;
import se.crisp.codekvast.server.agent.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.UserDAO;
import se.crisp.codekvast.server.codekvast_server.exception.CodekvastException;

import javax.inject.Inject;
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
public class AgentDAOImpl implements AgentDAO {

    private final JdbcTemplate jdbcTemplate;
    private final UserDAO userDAO;

    @Inject
    public AgentDAOImpl(JdbcTemplate jdbcTemplate, UserDAO userDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDAO = userDAO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void storeJvmData(JvmData data) throws CodekvastException {
        long customerId = userDAO.getCustomerId(data.getHeader().getCustomerName());
        long appId = userDAO.getAppId(customerId, data.getHeader().getEnvironment(), data.getAppName(), data.getAppVersion());
        storeJvmData(customerId, appId, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Collection<InvocationEntry> storeInvocationData(InvocationData invocationData) {
        final Collection<InvocationEntry> result = new ArrayList<>();

        UserDAO.AppId appId = userDAO.getAppIdByJvmFingerprint(invocationData.getJvmFingerprint());

        for (InvocationEntry entry : invocationData.getInvocations()) {
            storeOrUpdateInvocationEntry(result, appId, invocationData.getJvmFingerprint(), entry);
        }

        return result;
    }

    private void storeOrUpdateInvocationEntry(Collection<InvocationEntry> result, UserDAOImpl.AppId appId, String jvmFingerprint,
                                              InvocationEntry entry) {
        Date invokedAt = entry.getInvokedAtMillis() == null ? null : new Date(entry.getInvokedAtMillis());
        Integer confidence = entry.getConfidence() == null ? null : entry.getConfidence().ordinal();

        int updated = attemptToUpdateSignature(appId, jvmFingerprint, entry, invokedAt, confidence);

        if (updated > 0) {
            log.trace("Updated {}", entry);
            result.add(entry);
            return;
        }

        try {
            jdbcTemplate.update("INSERT INTO SIGNATURES(CUSTOMER_ID, APPLICATION_ID, SIGNATURE, JVM_FINGERPRINT, INVOKED_AT, CONFIDENCE) " +
                                        "VALUES(?, ?, ?, ?, ?, ?)",
                                appId.getCustomerId(), appId.getAppId(), entry.getSignature(), jvmFingerprint, invokedAt, confidence);
            log.trace("Stored {}", entry);
            result.add(entry);
        } catch (Exception ignore) {
            log.trace("Ignored attempt to insert duplicate signature");
        }
    }

    private int attemptToUpdateSignature(UserDAOImpl.AppId appId, String jvmFingerprint, InvocationEntry entry, Date invokedAt,
                                         Integer confidence) {
        if (invokedAt == null) {
            // An uninvoked signature is not allowed to overwrite an invoked signature
            return jdbcTemplate.update("UPDATE SIGNATURES SET CONFIDENCE = ? " +
                                               "WHERE CUSTOMER_ID = ? AND APPLICATION_ID = ? AND SIGNATURE = ? AND INVOKED_AT IS NULL ",
                                       confidence, appId.getCustomerId(), appId.getAppId(), entry.getSignature());
        }

        // An invocation. Overwrite whatever was there.
        return jdbcTemplate.update("UPDATE SIGNATURES SET INVOKED_AT = ?, JVM_FINGERPRINT = ?, CONFIDENCE = ? " +
                                           "WHERE CUSTOMER_ID = ? AND APPLICATION_ID = ? AND SIGNATURE = ? ",
                                   invokedAt, jvmFingerprint, confidence, appId.getCustomerId(), appId.getAppId(), entry.getSignature());

    }

    private void storeJvmData(long customerId, long appId, JvmData data) {
        Date dumpedAt = new Date(data.getDumpedAtMillis());

        int updated =
                jdbcTemplate
                        .update("UPDATE JVM_RUNS SET DUMPED_AT = ? WHERE CUSTOMER_ID = ? AND APPLICATION_ID = ? AND JVM_FINGERPRINT = ?",
                                dumpedAt, customerId, appId, data.getJvmFingerprint());
        if (updated > 0) {
            log.debug("Updated dumped_at={} for JVM run {}", dumpedAt, data.getJvmFingerprint());
            return;
        }

        int inserted =
                jdbcTemplate
                        .update("INSERT INTO JVM_RUNS(CUSTOMER_ID, APPLICATION_ID, HOST_NAME, JVM_FINGERPRINT, CODEKVAST_VERSION, " +
                                        "CODEKVAST_VCS_ID, STARTED_AT, DUMPED_AT)" +
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

}
