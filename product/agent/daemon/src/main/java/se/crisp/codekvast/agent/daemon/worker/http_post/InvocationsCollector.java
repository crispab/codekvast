package se.crisp.codekvast.agent.daemon.worker.http_post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.daemon.DaemonConstants;
import se.crisp.codekvast.agent.lib.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.daemon_api.model.v1.SignatureEntry;

import javax.inject.Inject;
import java.util.List;

@Repository
@Profile(DaemonConstants.HTTP_POST_PROFILE)
@Slf4j
class InvocationsCollector {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    InvocationsCollector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void put(String jvmUuid, long jvmStartedAtMillis, String signature, long invokedAtMillis,
                    SignatureConfidence confidence) {
        if (jvmUuid == null) {
            throw new IllegalArgumentException("jvmUuid is null");
        }

        if (jvmStartedAtMillis <= 0) {
            throw new IllegalArgumentException("jvmStartedAtMillis must be positive");
        }

        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }

        if (invokedAtMillis < 0) {
            throw new IllegalArgumentException("invokedAtMillis cannot be negative");
        }

        if (invokedAtMillis > 0 && invokedAtMillis < jvmStartedAtMillis) {
            log.debug("Ignoring invocation with invokedAtMillis {} before jvmStartedAtMillis {}", invokedAtMillis, jvmStartedAtMillis);
            return;
        }

        int count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures WHERE jvm_uuid = ? AND signature = ? AND " +
                                                        "invoked_at_millis > ?", Integer.class, jvmUuid, signature, invokedAtMillis);

        if (count == 0) {
            long millisSinceJvmStart = invokedAtMillis == 0L ? 0L : invokedAtMillis - jvmStartedAtMillis;
            jdbcTemplate.update("MERGE INTO signatures (jvm_uuid, signature, invoked_at_millis, millis_since_jvm_start, confidence) " +
                                        "VALUES(?, ?, ?, ?, ?)",
                                jvmUuid, signature, invokedAtMillis, millisSinceJvmStart, confidence == null ? -1 : confidence.ordinal());
        }
    }

    public List<SignatureEntry> getNotUploadedInvocations(String jvmUuid) {
        return jdbcTemplate.query("SELECT signature, invoked_at_millis, millis_since_jvm_start, confidence " +
                                          "FROM signatures " +
                                          "WHERE jvm_uuid = ?",
                                  (rs, rowNum) -> {
                                      Integer confidence = rs.getInt(4);
                                      return new SignatureEntry(
                                              rs.getString(1),
                                              rs.getLong(2),
                                              rs.getLong(3),
                                              confidence == -1 ? null : se.crisp.codekvast.server.daemon_api.model.v1.SignatureConfidence
                                                      .fromOrdinal(confidence));
                                  },
                                  jvmUuid);
    }

    @Transactional
    public void clearNotUploadedSignatures(String jvmUuid) {
        jdbcTemplate.update("DELETE FROM signatures WHERE jvm_uuid = ?", jvmUuid);
    }
}
