package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.util.DatabaseUtils;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;

/**
 * Performs database backups.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class DatabaseBackupScheduler {

    private static final String FILENAME_SUFFIX = "scheduled";
    private final JdbcTemplate jdbcTemplate;
    private final CodekvastSettings settings;

    @Inject
    public DatabaseBackupScheduler(JdbcTemplate jdbcTemplate, CodekvastSettings settings) {
        this.jdbcTemplate = jdbcTemplate;
        this.settings = settings;
    }

    @Scheduled(cron = "${codekvast.backupSchedule}")
    public void createBackup() throws SQLException {
        if (DatabaseUtils.isMemoryDatabase(jdbcTemplate)) {
            log.debug("Not backing up a memory database");
        } else {
            long startedAt = System.currentTimeMillis();

            String backupFile = DatabaseUtils.getBackupFile(settings, jdbcTemplate, new Date(), FILENAME_SUFFIX);
            log.debug("Backing up database to {}", backupFile);
            DatabaseUtils.backupDatabase(jdbcTemplate, backupFile);
            log.info("Database backed up to {} in {} ms", backupFile, System.currentTimeMillis() - startedAt);

            DatabaseUtils.removeOldBackups(settings, FILENAME_SUFFIX);
        }
    }

}
