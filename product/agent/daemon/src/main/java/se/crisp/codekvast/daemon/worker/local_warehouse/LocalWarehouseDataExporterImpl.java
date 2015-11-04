package se.crisp.codekvast.daemon.worker.local_warehouse;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.DaemonConstants;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.worker.DataExportException;
import se.crisp.codekvast.daemon.worker.DataExporter;
import se.crisp.codekvast.shared.util.FileUtils;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
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
@Profile("localWarehouse")
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
        if (config.getExportFile() == null) {
            log.info("No export file configured, data will not be exported");
            return;
        }

        if (!config.getExportFile().getName().toLowerCase().endsWith(".zip")) {
            log.warn("Can only export to ZIP format");
            return;
        }

        Instant startedAt = now();

        doExportDataTo(config.getExportFile());

        log.info("Created {} ({}) in {} s", config.getExportFile(), humanReadableByteCount(config.getExportFile().length()),
                 Duration.between(startedAt, now()).getSeconds());
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
        log.info("Exporting data to {}", exportFile);

        File tmpFile = createTempFile(exportFile);

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            zip.setComment("Export of Codekvast local warehouse for " + config.getEnvironment() + " at " + Instant.now());

            Charset charset = Charset.forName("UTF-8");
            doExportDaemonConfig(zip, charset, config);

            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zip, charset));
            doExportDatabaseTable(zip, csvWriter, "applications", "id", "name", "version", "createdAtMillis");
            doExportDatabaseTable(zip, csvWriter, "methods", "id", "visibility", "signature", "createdAtMillis");
            doExportDatabaseTable(zip, csvWriter, "jvms", "id", "uuid", "startedAtMillis", "dumpedAtMillis", "jsonData");
            doExportDatabaseTable(zip, csvWriter, "invocations", "applicationId", "methodId", "jvmId", "invokedAtMillis", "invocationCount",
                                  "confidence", "exportedAtMillis");

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
        zip.putNextEntry(new ZipEntry(DaemonConstants.DAEMON_CONFIG_FILE));

        Set<String> lines = new TreeSet<>();
        FileUtils.extractFieldValuesFrom(config, lines);
        for (String line : lines) {
            zip.write(line.getBytes(charset));
            zip.write('\n');
        }
        zip.closeEntry();
    }

    private void doExportDatabaseTable(ZipOutputStream zip, CSVWriter csvWriter, String table, String... columns) throws IOException {
        zip.putNextEntry(new ZipEntry(table + ".csv"));
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
        try {
            return File.createTempFile("codekvast-export", ".tmp", file.getParentFile());
        } catch (IOException e) {
            throw new DataExportException("Cannot create temporary file in " + file.getParentFile(), e);
        }
    }
}
