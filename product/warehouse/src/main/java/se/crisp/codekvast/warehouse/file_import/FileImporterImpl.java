package se.crisp.codekvast.warehouse.file_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileEntry;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileFormat;
import se.crisp.codekvast.warehouse.config.CodekvastSettings;

import javax.inject.Inject;
import java.io.*;
import java.util.IllegalFormatException;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scans a certain directory for files produced by the Codekvast daemon, and attempts to import them to the database.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class FileImporterImpl {

    private final CodekvastSettings codekvastSettings;
    private final ImportService importService;

    @Inject
    public FileImporterImpl(CodekvastSettings codekvastSettings, ImportService importService) {
        this.codekvastSettings = codekvastSettings;
        this.importService = importService;
        log.info("Created");
    }

    @Scheduled(fixedDelayString = "${codekvast.importPollIntervalSeconds}000")
    public void importDaemonFiles() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("FileImport");
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

        Properties daemonProperties = null;
        String uuid;

        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zin.getNextEntry()) != null) {
                ExportFileEntry exportFileEntry = ExportFileEntry.fromString(zipEntry.getName());
                log.debug("Reading {}...", exportFileEntry);

                switch (exportFileEntry) {
                case DAEMON_CONFIG:
                    daemonProperties = loadDaemonProperties(zin);
                    uuid = daemonProperties.getProperty("exportUuid");
                    if (importService.isFileImported(uuid)) {
                        log.debug("{} with uuid {} has already been imported", file, uuid);
                        return;
                    }
                    break;
                case APPLICATIONS:
                    assertDaemonConfigEntry(file, daemonProperties);
                    break;
                case METHODS:
                    break;
                case JVMS:
                    break;
                case INVOCATIONS:
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Cannot import " + file, e);
        } catch (IOException e) {
            log.error("Cannot import " + file, e);
        }
    }

    private void assertDaemonConfigEntry(File file, Properties daemonProperties) {
        if (daemonProperties == null) {
            throw new IllegalArgumentException(String.format("Missing %s in %s", ExportFileEntry.DAEMON_CONFIG.getEntryName(), file));
        }
    }

    private Properties loadDaemonProperties(InputStream inputStream) throws IOException {
        Properties props = new Properties();
        props.load(inputStream);
        return props;
    }
}
