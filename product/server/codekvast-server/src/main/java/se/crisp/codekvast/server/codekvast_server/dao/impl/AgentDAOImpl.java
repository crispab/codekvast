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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    public void storeInvocationData(AppId appId, InvocationData invocationData) {

        Set<String> existing =
                new HashSet<>(jdbcTemplate
                                      .queryForList("SELECT signature FROM signatures WHERE application_id = ? AND organisation_id = ? ",
                                                    String.class, appId.getAppId(), appId.getOrganisationId()));

        String jvmFingerprint = invocationData.getJvmFingerprint();

        for (InvocationEntry entry : invocationData.getInvocations()) {
            Integer confidence = entry.getConfidence() == null ? null : entry.getConfidence().ordinal();

            if (existing.contains(entry.getSignature())) {
                if (entry.getInvokedAtMillis() == null) {
                    // An uninvoked signature is not allowed to overwrite an invoked signature
                    jdbcTemplate
                            .update("UPDATE signatures SET confidence = ? " +
                                            "WHERE signature = ? AND application_id = ? AND invoked_at IS NULL ",
                                    confidence, entry.getSignature(), appId.getAppId());
                } else {
                    // An invocation. Overwrite whatever was there.
                    jdbcTemplate
                            .update("UPDATE signatures SET invoked_at = ?, jvm_fingerprint = ?, confidence = ? " +
                                            "WHERE signature = ? AND application_id = ? ",
                                    entry.getInvokedAtMillis(), jvmFingerprint, confidence, entry.getSignature(), appId.getAppId());
                }
                log.trace("Updated {}", entry);
            } else {
                jdbcTemplate
                        .update("INSERT INTO signatures(organisation_id, application_id, signature, jvm_fingerprint, invoked_at, " +
                                        "confidence) " +
                                        "VALUES(?, ?, ?, ?, ?, ?)",
                                appId.getOrganisationId(), appId.getAppId(), entry.getSignature(), jvmFingerprint,
                                entry.getInvokedAtMillis(), confidence);
                log.trace("Stored {}", entry);
            }
        }
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
                .update("INSERT INTO jvm_runs(organisation_id, application_id, application_version, computer_id, host_name, jvm_fingerprint, " +
                                "codekvast_version, " +
                                "codekvast_vcs_id, started_at, dumped_at)" +
                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        organisationId, appId, data.getAppVersion(), data.getComputerId(), data.getHostName(), data.getJvmFingerprint(),
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
