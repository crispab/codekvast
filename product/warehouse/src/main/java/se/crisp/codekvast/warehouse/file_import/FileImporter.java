package se.crisp.codekvast.warehouse.file_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.warehouse.config.CodekvastSettings;

import javax.inject.Inject;

/**
 * Scans a certain directory for files produced by the Codekvast daemon, and attempts to import them to the database.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class FileImporter {

    private final CodekvastSettings codekvastSettings;
    private final JdbcTemplate jdbcTemplate;

    @Inject
    public FileImporter(CodekvastSettings codekvastSettings, JdbcTemplate jdbcTemplate) {
        this.codekvastSettings = codekvastSettings;
        this.jdbcTemplate = jdbcTemplate;
        log.info("Created");
    }

    @Scheduled(fixedDelayString = "${codekvast.importPollIntervalSeconds}000")
    public void importDaemonFiles() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(FileImporter.class.getSimpleName());
        try {
            log.debug("Looking for import files in {}", codekvastSettings.getImportPath());

        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }
}
