package se.crisp.codekvast.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Database maintenance functions.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class DatabaseMaintenanceImpl {

    private static final String BACKUP_SUFFIX = ".h2.zip";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Value("${codekvast.web.database-backup.path}")
    private File databaseBackupPath;

    @Autowired
    @Value("${codekvast.web.database-backup.max}")
    private Integer databaseBackupMax;

    @Autowired
    @Value("${codekvast.web.csv-export.file}")
    private File csvExportFile;

    @Scheduled(cron = "${codekvast.web.database-backup.cron}")
    public void makeDatabaseBackup() {
        String dumpFile = getDumpFile(databaseBackupPath, new Date(), BACKUP_SUFFIX).getAbsolutePath();
        try {
            long startedAt = System.currentTimeMillis();

            jdbcTemplate.update("BACKUP TO ?", dumpFile);

            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("Backed up database to {} in {} s", dumpFile, elapsed / 1000L);
        } catch (DataAccessException e) {
            log.error("Failed to backup database to " + dumpFile, e);
        }

        removeOldBackups(databaseBackupPath, databaseBackupMax, BACKUP_SUFFIX);
    }

    @Scheduled(cron = "${codekvast.web.csv-export.cron}")
    public void exportCsvFile() {
        String destination = csvExportFile.getAbsolutePath();
        jdbcTemplate.update("CALL CSVWRITE(?, 'SELECT * FROM PEOPLE', 'charset=UTF-8')", destination);
        log.info("Exported people into {}", destination);
    }

    static void removeOldBackups(File backupPath, int maxBackups, final String suffix) {
        File[] files = backupPath.listFiles((file, name) -> { return name.endsWith(suffix); });

        Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

        int filesToDelete = files.length - maxBackups;
        if (filesToDelete > 0) {
            for (int i = 0; i < filesToDelete; i++) {
                boolean deleted = files[i].delete();
                if (deleted) {
                    log.debug("Deleted {}", files[i]);
                }
            }
        }
    }

    static File getDumpFile(File backupPath, Date date, String suffix) {
        return new File(backupPath, String.format(Locale.ENGLISH, "%1$tF_%1$tT%2$s", date, suffix).replaceAll("[-:]", ""));
    }

}
