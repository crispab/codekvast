package se.crisp.codekvast.server.codekvast_server.service.impl;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.server.codekvast_server.event.internal.InvocationDataReceivedEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class DatabaseScavenger {
    private static final String TMP_TABLE = DatabaseScavenger.class.getSimpleName().toLowerCase();

    private final JdbcTemplate jdbcTemplate;
    private final EventBus eventBus;

    private volatile boolean needsScavenging = true;

    @Inject
    public DatabaseScavenger(JdbcTemplate jdbcTemplate, EventBus eventBus) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void postConstruct() {
        eventBus.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onInvocationDataReceivedEvent(InvocationDataReceivedEvent event) {
        needsScavenging = true;
    }

    @Scheduled(initialDelay = 60_000L, fixedDelay = 300_000L)
    @Transactional(rollbackFor = Exception.class)
    public void removeOldSignatures() {
        String savedThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(getClass().getSimpleName());
            if (!needsScavenging) {
                log.debug("No scavenging needed");
            } else {
                doRemoveOldSignatures();
                needsScavenging = false;
            }
        } finally {
            Thread.currentThread().setName(savedThreadName);
        }
    }

    private void doRemoveOldSignatures() {
        long startedAt = System.currentTimeMillis();
        log.debug("Looking for garbage signature rows...");

        int rowsToKeep = 0;
        List<Long> rowsToDelete = new ArrayList<>();

        // Place a table lock on signatures...
        List<TmpSignature> allSignatures = jdbcTemplate.query(
                "SELECT id, organisation_id, signature, invoked_at FROM signatures ORDER BY invoked_at DESC FOR UPDATE ",
                new TmpSignatureRowMapper());

        Set<TmpSignature> newestSignatures = new HashSet<>();
        for (TmpSignature sig : allSignatures) {
            if (newestSignatures.add(sig)) {
                rowsToKeep += 1;
            } else {
                // There was already an equal object in newestSignatures, i.e., with same organisation_id and signature.
                // The ORDER BY clause guarantees that this first one has the highest invoked_at value.
                log.trace("Found garbage row {}", sig);
                rowsToDelete.add(sig.getId());
            }
        }

        if (!rowsToDelete.isEmpty()) {
            log.debug("Will delete {} garbage signature rows...", rowsToDelete.size());

            // A quite clumsy way of doing it. The more obvious way,
            // jdbcTemplate.batchUpdate("DELETE FROM signatures WHERE id = ?", rowsToDelete)
            // performs lousy.

            fillTemporaryTableWith(rowsToDelete);

            int deleted =
                    jdbcTemplate.update("DELETE FROM signatures s WHERE EXISTS(SELECT id FROM " + TMP_TABLE + " t WHERE t.id = s.id )");

            log.info("Deleted {} and kept {} signatures in {} ms", deleted, rowsToKeep, System.currentTimeMillis() - startedAt);
        }
    }

    private void fillTemporaryTableWith(List<Long> ids) {
        jdbcTemplate.update("CREATE MEMORY LOCAL TEMPORARY TABLE IF NOT EXISTS " + TMP_TABLE +
                                    "(id BIGINT PRIMARY KEY) NOT PERSISTENT TRANSACTIONAL ");

        jdbcTemplate.update("DELETE FROM " + TMP_TABLE);

        jdbcTemplate.batchUpdate("INSERT INTO " + TMP_TABLE + "(id) VALUES (?)", ids, 500,
                                 new ParameterizedPreparedStatementSetter<Long>() {
                                     @Override
                                     public void setValues(PreparedStatement ps, Long argument) throws SQLException {
                                         ps.setLong(1, argument);
                                     }
                                 });
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
