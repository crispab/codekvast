package se.crisp.codekvast.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.Locale;

/**
 * Database maintenance functions.
 *
 * @author Olle Hallin (qolha), olle.hallin@crisp.se
 */
@Service
@Slf4j
public class DatabaseMaintenanceImpl {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Value("${codekvast.web.database.backupPath}")
    private File databaseBackupPath;

    @Scheduled(cron = "${codekvast.web.database.cron}")
    public void makeDatabaseBackup() {
        File dumpFile = getDumpFile(databaseBackupPath, new Date());
        log.info("Backing up database to {}", dumpFile);
    }

    static File getDumpFile(File backupPath, Date date) {
        return new File(backupPath, String.format(Locale.ENGLISH, "%1$tF_%1$tT.h2.dmp", date).replaceAll("[-:]", ""));
    }

}
