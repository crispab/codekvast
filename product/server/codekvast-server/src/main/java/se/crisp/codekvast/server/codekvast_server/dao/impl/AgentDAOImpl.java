package se.crisp.codekvast.server.codekvast_server.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationData;
import se.crisp.codekvast.server.agent_api.model.v1.InvocationEntry;
import se.crisp.codekvast.server.agent_api.model.v1.JvmData;
import se.crisp.codekvast.server.codekvast_server.dao.AgentDAO;
import se.crisp.codekvast.server.codekvast_server.dao.CollectorTimestamp;
import se.crisp.codekvast.server.codekvast_server.model.AppId;

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
public class AgentDAOImpl implements AgentDAO {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public AgentDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<InvocationEntry> storeInvocationData(AppId appId, InvocationData invocationData) {
        final Collection<InvocationEntry> result = new ArrayList<>();

        for (InvocationEntry entry : invocationData.getInvocations()) {
            storeOrUpdateInvocationEntry(result, appId, invocationData.getJvmFingerprint(), entry);
        }

        return result;
    }

    private void storeOrUpdateInvocationEntry(Collection<InvocationEntry> result, AppId appId, String jvmFingerprint,
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
            jdbcTemplate
                    .update("INSERT INTO SIGNATURES(ORGANISATION_ID, APPLICATION_ID, SIGNATURE, JVM_FINGERPRINT, INVOKED_AT, CONFIDENCE) " +
                                        "VALUES(?, ?, ?, ?, ?, ?)",
                            appId.getOrganisationId(), appId.getAppId(), entry.getSignature(), jvmFingerprint, invokedAt, confidence);
            log.trace("Stored {}", entry);
            result.add(entry);
        } catch (Exception ignore) {
            log.trace("Ignored attempt to insert duplicate signature");
        }
    }

    private int attemptToUpdateSignature(AppId appId, String jvmFingerprint, InvocationEntry entry, Date invokedAt,
                                         Integer confidence) {
        if (invokedAt == null) {
            // An uninvoked signature is not allowed to overwrite an invoked signature
            return jdbcTemplate.update("UPDATE SIGNATURES SET CONFIDENCE = ? " +
                                               "WHERE APPLICATION_ID = ? AND SIGNATURE = ? AND INVOKED_AT IS NULL ",
                                       confidence, appId.getAppId(), entry.getSignature());
        }

        // An invocation. Overwrite whatever was there.
        return jdbcTemplate.update("UPDATE SIGNATURES SET INVOKED_AT = ?, JVM_FINGERPRINT = ?, CONFIDENCE = ? " +
                                           "WHERE APPLICATION_ID = ? AND SIGNATURE = ? ",
                                   invokedAt, jvmFingerprint, confidence, appId.getAppId(), entry.getSignature());
    }

    @Override
    public void storeJvmData(long organisationId, long appId, JvmData data) {
        int updated =
                jdbcTemplate
                        .update("UPDATE jvm_runs SET dumped_at = ? WHERE application_id = ? AND jvm_fingerprint = ?",
                                data.getDumpedAtMillis(), appId, data.getJvmFingerprint());
        if (updated > 0) {
            log.debug("Updated dumped_at={} for JVM run {}", new Date(data.getDumpedAtMillis()), data.getJvmFingerprint());
            return;
        }

        updated = jdbcTemplate
                .update("INSERT INTO jvm_runs(organisation_id, application_id, application_version, host_name, jvm_fingerprint, " +
                                "codekvast_version, " +
                                "codekvast_vcs_id, started_at, dumped_at)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        organisationId, appId, data.getAppVersion(), data.getHostName(), data.getJvmFingerprint(),
                        data.getCodekvastVersion(), data.getCodekvastVcsId(), data.getStartedAtMillis(),
                        data.getDumpedAtMillis());

        if (updated == 1) {
            log.debug("Stored new JVM run {}", data);
        } else {
            log.warn("Cannot store JVM run {}", data);
        }
    }

    @Override
    public CollectorTimestamp getCollectorTimestamp(long organisationId) {

        return jdbcTemplate.queryForObject("SELECT MIN(started_at), MAX(dumped_at) FROM jvm_runs WHERE organisation_id = ? ",
                                           new CollectorTimestampRowMapper(), organisationId);
    }

    private static class CollectorTimestampRowMapper implements RowMapper<CollectorTimestamp> {
        @Override
        public CollectorTimestamp mapRow(ResultSet rs, int rowNum) throws SQLException {
            // SELECT MIN(started_at), MAX(dumped_at) FROM jvm_runs
            return CollectorTimestamp.builder()
                                     .startedAtMillis(rs.getLong(1))
                                     .dumpedAtMillis(rs.getLong(2))
                                     .build();
        }
    }
}
