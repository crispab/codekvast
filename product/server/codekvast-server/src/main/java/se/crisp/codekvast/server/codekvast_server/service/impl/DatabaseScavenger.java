package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.PreparedStatement;
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
    private static final String TMP_TABLE = DatabaseScavenger.class.getSimpleName().toLowerCase();

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public DatabaseScavenger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(initialDelay = 10_000L, fixedDelay = 60_000L)
    @Transactional
    public void removeOldSignatures() {
        String savedThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(getClass().getSimpleName());
            long startedAt = System.currentTimeMillis();
            log.trace("Looking for garbage signature rows...");

            List<Long> rowsToKeep = new ArrayList<>();
            List<Long> rowsToDelete = new ArrayList<>();

            List<TmpSignature> allSignatures = jdbcTemplate.query(
                    "SELECT id, organisation_id, signature, invoked_at FROM signatures ORDER BY invoked_at DESC ",
                    new TmpSignatureRowMapper());

            Set<TmpSignature> newestSignatures = new HashSet<>();
            for (TmpSignature sig : allSignatures) {
                if (newestSignatures.add(sig)) {
                    rowsToKeep.add(sig.getId());
                } else {
                    // There was already an equal object in newestSignatures, i.e., with same organisation_id and signature.
                    // The ORDER BY clause guarantees that this first one has the highest invoked_at value.
                    log.trace("Found garbage row {}", sig);
                    rowsToDelete.add(sig.getId());
                }
            }

            if (!rowsToDelete.isEmpty()) {
                log.debug("Will delete {} garbage signature rows...", rowsToDelete.size());

                fillTemporaryTableWith(rowsToDelete);

                int deleted =
                        jdbcTemplate.update("DELETE FROM signatures s WHERE EXISTS(SELECT ID FROM " + TMP_TABLE + " t WHERE t.id = s.id )");
                log.debug("Deleted {} garbage signature rows in {} ms (kept {} rows)", deleted, System.currentTimeMillis() -
                        startedAt, rowsToKeep.size());
            }
        } finally {
            Thread.currentThread().setName(savedThreadName);
        }
    }

    private int fillTemporaryTableWith(List<Long> ids) {
        jdbcTemplate.update("CREATE MEMORY LOCAL TEMPORARY TABLE IF NOT EXISTS " + TMP_TABLE +
                                    " (id BIGINT PRIMARY KEY ) NOT PERSISTENT TRANSACTIONAL ");

        jdbcTemplate.update("DELETE FROM " + TMP_TABLE);

        int[][] updates = jdbcTemplate.batchUpdate("INSERT INTO " + TMP_TABLE + " SET id = ?", ids, 100,
                                                   new ParameterizedPreparedStatementSetter<Long>() {
                                                       @Override
                                                       public void setValues(PreparedStatement ps, Long argument) throws SQLException {
                                                           ps.setLong(1, argument);
                                                       }
                                                   });

        int inserted = 0;
        for (int i = 0; i < updates.length; i++) {
            for (int j = 0; j < updates[i].length; j++) {
                inserted += updates[i][j];
            }
        }
        return inserted;
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
