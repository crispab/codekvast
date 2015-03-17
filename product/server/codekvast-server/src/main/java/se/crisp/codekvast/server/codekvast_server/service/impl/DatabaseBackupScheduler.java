package se.crisp.codekvast.server.codekvast_server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;
import se.crisp.codekvast.server.codekvast_server.util.DatabaseUtils;

import javax.inject.Inject;
import javax.sql.DataSource;
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

    private final DataSource dataSource;
    private final CodekvastSettings settings;

    @Inject
    public DatabaseBackupScheduler(DataSource dataSource, CodekvastSettings settings) {
        this.dataSource = dataSource;
        this.settings = settings;
    }

    @Scheduled(cron = "${codekvast.backupSchedule}")
    public void createBackup() throws SQLException {
        if (DatabaseUtils.isMemoryDatabase(dataSource)) {
            log.debug("Not backing up a memory database");
        } else {
            long startedAt = System.currentTimeMillis();

            String backupFile = DatabaseUtils.getBackupFile(settings, new Date(), "scheduled");
            log.debug("Backing up database to {}", backupFile);

            DatabaseUtils.backupDatabase(dataSource, backupFile);

            log.info("Database backed up to {} in {} ms", backupFile, System.currentTimeMillis() - startedAt);
        }
    }
}
