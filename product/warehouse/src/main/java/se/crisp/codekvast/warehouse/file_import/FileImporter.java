package se.crisp.codekvast.warehouse.file_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileEntry;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileFormat;
import se.crisp.codekvast.warehouse.config.CodekvastSettings;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            walkDirectory(codekvastSettings.getImportPath());
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    private void walkDirectory(File path) {
        if (path != null) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        walkDirectory(file);
                    } else {
                        tryToImport(file);
                    }
                }
            }
        }
    }

    private void tryToImport(File file) {
        log.debug("Trying to import {}", file);
        if (!file.getName().endsWith(ExportFileFormat.ZIP.getSuffix())) {
            log.debug("Ignoring {}, can only handle {} files", file, ExportFileFormat.ZIP);
            return;
        }

        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zin.getNextEntry()) != null) {
                ExportFileEntry exportFileEntry = ExportFileEntry.fromString(zipEntry.getName());
                log.debug("Parsing {}...", exportFileEntry);
            }
        } catch (IOException e) {
            log.error("Cannot import " + file, e);
        }
    }
}
