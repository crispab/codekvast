package se.crisp.codekvast.agent.daemon.worker.local_warehouse;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.DaemonConstants;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.worker.DataExportException;
import se.crisp.codekvast.agent.daemon.worker.DataExporter;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileEntry;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileFormat;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

/**
 * An implementation of DataExporter that exports invocation data from a local data warehouse. <p> It produces a self-contained zip file
 * containing a number of CSV files, one for each table in the database. </p>
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Profile(DaemonConstants.LOCAL_WAREHOUSE_PROFILE)
@Slf4j
public class LocalWarehouseDataExporterImpl implements DataExporter {

    private final JdbcTemplate jdbcTemplate;
    private final DaemonConfig config;

    @Inject
    public LocalWarehouseDataExporterImpl(JdbcTemplate jdbcTemplate, DaemonConfig config) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    @Override
    public void exportData() throws DataExportException {
        File exportFile = expandPlaceholders(config.getExportFile());
        if (exportFile == null) {
            log.info("No export file configured, data will not be exported");
            return;
        }

        if (!exportFile.getName().toLowerCase().endsWith(ExportFileFormat.ZIP.getSuffix())) {
            log.error("Can only export to " + ExportFileFormat.ZIP + " format");
            return;
        }

        Instant startedAt = now();

        doExportDataTo(exportFile);

        log.info("Created {} ({}) in {} s", exportFile, humanReadableByteCount(exportFile.length()),
                 Duration.between(startedAt, now()).getSeconds());
    }

    private File expandPlaceholders(File file) {
        if (file == null) {
            return null;
        }

        String timestamp = now().toString();

        String name = file.getName()
                          .replace("#{timestamp}", timestamp)
                          .replace("#timestamp", timestamp)
                          .replace("#{datetime}", timestamp)
                          .replace("#datetime", timestamp);
        return new File(file.getParentFile(), name);
    }

    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exponent = (int) (Math.log(bytes) / Math.log(1000));
        String unit = " kMGTPE".charAt(exponent) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1000, exponent), unit);
    }

    private void doExportDataTo(File exportFile) throws DataExportException {
        log.debug("Exporting data to {} ...", exportFile);

        File tmpFile = createTempFile(exportFile);

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            String exportUuid = UUID.randomUUID().toString();

            zip.setComment(
                    "Export of Codekvast local warehouse for " + config.getEnvironment() + " at " + Instant.now() + ", uuid=" + exportUuid);

            Charset charset = Charset.forName("UTF-8");
            doExportDaemonConfig(zip, charset, config.withExportUuid(exportUuid));

            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zip, charset));
            doExportDatabaseTable(zip, csvWriter, "applications", "id", "name", "version", "createdAtMillis");
            doExportDatabaseTable(zip, csvWriter, "methods", "id", "visibility", "signature", "createdAtMillis", "declaringType",
                                  "exceptionTypes", "methodName", "modifiers", "packageName", "parameterTypes", "returnType");
            doExportDatabaseTable(zip, csvWriter, "jvms", "id", "uuid", "startedAtMillis", "dumpedAtMillis", "jvmDataJson");
            doExportDatabaseTable(zip, csvWriter, "invocations", "applicationId", "methodId", "jvmId", "invokedAtMillis", "invocationCount",
                                  "confidence");

            zip.finish();
        } catch (Exception e) {
            throw new DataExportException("Cannot create " + exportFile, e);
        }

        if (!tmpFile.renameTo(exportFile)) {
            tmpFile.delete();
            throw new DataExportException(String.format("Cannot rename %s to %s", tmpFile, exportFile));
        }
    }

    private void doExportDaemonConfig(ZipOutputStream zip, Charset charset, DaemonConfig config)
            throws IOException, IllegalAccessException {
        zip.putNextEntry(ExportFileEntry.DAEMON_CONFIG.toZipEntry());

        Set<String> lines = new TreeSet<>();
        FileUtils.extractFieldValuesFrom(config, lines);
        for (String line : lines) {
            zip.write(line.getBytes(charset));
            zip.write('\n');
        }
        zip.closeEntry();
    }

    private void doExportDatabaseTable(ZipOutputStream zip, CSVWriter csvWriter, String table, String... columns) throws IOException {
        zip.putNextEntry(ExportFileEntry.fromString(table + ".csv").toZipEntry());
        csvWriter.writeNext(columns, false);

        String[] line = new String[columns.length];
        jdbcTemplate.query("SELECT " + asList(columns).stream().collect(joining(", ")) + " FROM " + table, rs -> {
            for (int i = 0; i < columns.length; i++) {
                line[i] = rs.getString(i + 1);
            }
            csvWriter.writeNext(line, false);
        });

        csvWriter.flush();
        zip.closeEntry();
    }

    private File createTempFile(File file) throws DataExportException {
        File directory = file.getParentFile();
        if (directory.mkdirs()) {
            log.info("Created {}", directory);
        }

        if (!directory.isDirectory()) {
            log.warn("Could not create {}", directory);
        }

        try {
            return File.createTempFile("codekvast-export", ".tmp", directory);
        } catch (IOException e) {
            throw new DataExportException("Cannot create temporary file in " + directory, e);
        }
    }
}
