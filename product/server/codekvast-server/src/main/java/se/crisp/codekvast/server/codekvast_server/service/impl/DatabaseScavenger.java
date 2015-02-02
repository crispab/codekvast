package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Olle Hallin <olle.hallin@crisp.se>
 */
@Component
@Slf4j
public class DatabaseScavenger {
    private final JdbcTemplate jdbcTemplate;

    @Inject
    public DatabaseScavenger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(initialDelay = 10_000L, fixedRate = 20_000L)
    public void removeOldSignatures() {
        log.trace("Looking for garbage signature rows...");

        List<Object[]> garbageIds = new ArrayList<>();

        Set<TmpSignature> keepSignatures = new HashSet<>();
        List<TmpSignature> allSignatures = jdbcTemplate.query(
                "SELECT id, organisation_id, signature, invoked_at FROM signatures ORDER BY organisation_id, signature, invoked_at DESC ",
                new SignatureRowMapper());

        for (TmpSignature sig : allSignatures) {
            if (!keepSignatures.add(sig)) {
                // There was already an equal object in keepSignatures, i.e., with same organisation_id and signature.
                // The ORDER BY clause guarantees that this first one has the highest invoked_at value.
                log.trace("Found garbage row {}", sig);
                garbageIds.add(new Object[]{sig.getId()});
            }
        }

        if (!garbageIds.isEmpty()) {
            log.debug("Will delete {} garbage signature rows...", garbageIds.size());

            int[] deletedInBatch = jdbcTemplate.batchUpdate("DELETE FROM signatures WHERE id = ?", garbageIds);

            int sum = 0;
            for (int d : deletedInBatch) {
                sum += d;
            }
            log.info("Deleted {} garbage signature rows", sum);
        }
    }

    @Value
    @EqualsAndHashCode(of = {"organisationId", "signature"})
    private class TmpSignature {
        private final long id;
        private final long organisationId;
        private final String signature;
        private final long invokedAt;
    }

    private class SignatureRowMapper implements RowMapper<TmpSignature> {
        @Override
        public TmpSignature mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TmpSignature(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4));
        }
    }
}
