package se.crisp.codekvast.agent.main;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureConfidence;
import se.crisp.codekvast.server.agent_api.model.v1.SignatureEntry;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
class InvocationsCollectorImpl implements InvocationsCollector {

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public InvocationsCollectorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void put(String jvmUuid, String signature, long invokedAtMillis, long millisSinceJvmStart, SignatureConfidence confidence) {
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }

        if (invokedAtMillis < 0L) {
            throw new IllegalArgumentException("invokedAtMillis cannot be negative");
        }

        int count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM signatures WHERE jvm_uuid = ? AND signature = ? AND " +
                                                        "invoked_at_millis > ?", Integer.class, jvmUuid, signature, invokedAtMillis);

        if (count == 0) {
            jdbcTemplate.update("MERGE INTO signatures (jvm_uuid, signature, invoked_at_millis, millis_since_jvm_start, confidence) " +
                                        "VALUES(?, ?, ?, ?, ?)",
                                jvmUuid, signature, invokedAtMillis, millisSinceJvmStart, confidence == null ? -1 : confidence.ordinal());
        }
    }

    @Override
    public List<SignatureEntry> getNotUploadedInvocations(String jvmUuid) {
        return jdbcTemplate.query("SELECT signature, invoked_at_millis, millis_since_jvm_start, confidence " +
                                          "FROM signatures " +
                                          "WHERE jvm_uuid = ?",
                                  new RowMapper<SignatureEntry>() {
                                      @Override
                                      public SignatureEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
                                          Integer confidence = rs.getInt(4);
                                          return new SignatureEntry(
                                                  rs.getString(1),
                                                  rs.getLong(2),
                                                  rs.getLong(2),
                                                  confidence == -1 ? null : SignatureConfidence.fromOrdinal(confidence));
                                      }
                                  },
                                  jvmUuid);
    }

    @Override
    @Transactional
    public void clearNotUploadedSignatures(String jvmUuid) {
        jdbcTemplate.update("DELETE FROM signatures WHERE jvm_uuid = ?", jvmUuid);
    }
}
