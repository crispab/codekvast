package se.crisp.codekvast.daemon.impl.local_warehouse;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.DaemonConstants;
import se.crisp.codekvast.daemon.DataExporter;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.impl.DataExportException;
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

        Instant startedAt = now();

        doExportDataTo(config.getExportFile());

        log.info("Created {} in {} s", config.getExportFile(), Duration.between(startedAt, now()).getSeconds());
    }

    private void doExportDataTo(File exportFile) throws DataExportException {
        log.info("Exporting data to {}", exportFile);

        File tmpFile = createTempFile(exportFile);

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            zos.setComment("Export of Codekvast local warehouse at " + Instant.now());

            Charset charset = Charset.forName("UTF-8");
            doExportDaemonConfig(zos, charset, config);

            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zos, charset));
            doExportDatabaseTable(zos, csvWriter, "applications", "id", "name", "version", "createdAtMillis");
            doExportDatabaseTable(zos, csvWriter, "methods", "id", "visibility", "signature", "createdAtMillis");
            doExportDatabaseTable(zos, csvWriter, "jvms", "id", "uuid", "startedAtMillis", "dumpedAtMillis", "jsonData");
            doExportDatabaseTable(zos, csvWriter, "invocations", "applicationId", "methodId", "jvmId", "invokedAtMillis", "invocationCount",
                                  "confidence", "exportedAtMillis");

            zos.finish();
        } catch (Exception e) {
            throw new DataExportException("Cannot create " + exportFile, e);
        }

        if (!tmpFile.renameTo(exportFile)) {
            tmpFile.delete();
            throw new DataExportException(String.format("Cannot rename %s to %s", tmpFile, exportFile));
        }
    }

    private void doExportDaemonConfig(ZipOutputStream zos, Charset charset, DaemonConfig config)
            throws IOException, IllegalAccessException {
        zos.putNextEntry(new ZipEntry(DaemonConstants.DAEMON_CONFIG_FILE));

        Set<String> lines = new TreeSet<>();
        FileUtils.extractFieldValuesFrom(config, lines);
        for (String line : lines) {
            zos.write(line.getBytes(charset));
            zos.write('\n');
        }
        zos.closeEntry();
    }

    private void doExportDatabaseTable(ZipOutputStream zos, CSVWriter csvWriter, String table, String... columns) throws IOException {
        zos.putNextEntry(new ZipEntry(table + ".csv"));
        csvWriter.writeNext(columns, false);

        String[] line = new String[columns.length];
        jdbcTemplate.query("SELECT " + asList(columns).stream().collect(joining(", ")) + " FROM " + table, rs -> {
            for (int i = 0; i < columns.length; i++) {
                line[i] = rs.getString(i + 1);
            }
            csvWriter.writeNext(line, false);
        });

        csvWriter.flush();
        zos.closeEntry();
    }

    private File createTempFile(File file) throws DataExportException {
        try {
            return File.createTempFile("codekvast-export", ".tmp", file.getParentFile());
        } catch (IOException e) {
            throw new DataExportException("Cannot create temporary file in " + file.getParentFile(), e);
        }
    }
}
