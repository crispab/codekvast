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

    @Scheduled(initialDelay = 15_000L, fixedRate = 120_000L)
    public void removeOldSignatures() {
        long startedAt = System.currentTimeMillis();
        log.trace("Looking for garbage signature rows...");

        List<Object[]> rowsToDelete = new ArrayList<>();

        List<TmpSignature> allSignatures = jdbcTemplate.query(
                "SELECT id, organisation_id, signature, invoked_at FROM signatures ORDER BY invoked_at DESC ",
                new TmpSignatureRowMapper());

        Set<TmpSignature> keepSignatures = new HashSet<>();
        for (TmpSignature sig : allSignatures) {
            if (!keepSignatures.add(sig)) {
                // There was already an equal object in keepSignatures, i.e., with same organisation_id and signature.
                // The ORDER BY clause guarantees that this first one has the highest invoked_at value.
                log.trace("Found garbage row {}", sig);
                rowsToDelete.add(new Object[]{sig.getId()});
            }
        }

        if (!rowsToDelete.isEmpty()) {
            log.debug("Will delete {} garbage signature rows...", rowsToDelete.size());

            int[] deletedInBatch = jdbcTemplate.batchUpdate("DELETE FROM signatures WHERE id = ?", rowsToDelete);

            int sum = 0;
            for (int d : deletedInBatch) {
                sum += d;
            }
            log.debug("Deleted {} garbage signature rows in {} ms", sum, System.currentTimeMillis() - startedAt);
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

    private class TmpSignatureRowMapper implements RowMapper<TmpSignature> {
        @Override
        public TmpSignature mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TmpSignature(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4));
        }
    }
}
