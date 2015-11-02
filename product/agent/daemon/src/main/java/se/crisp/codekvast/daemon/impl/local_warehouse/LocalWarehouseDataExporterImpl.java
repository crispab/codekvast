package se.crisp.codekvast.daemon.impl.local_warehouse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.DataExporter;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.impl.DataExportException;
import se.crisp.codekvast.shared.util.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.time.Instant.now;

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

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpFile));
        ) {
            Charset charset = Charset.forName(FileUtils.UTF_8);

            zos.setComment("Export of Codekvast local warehouse at " + Instant.now());

            ZipEntry zipEntry = new ZipEntry("daemon-config.properties");
            zos.putNextEntry(zipEntry);
            writeDaemonConfig(zos, config, charset);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("jvms.dat"));
            zos.write("Line1 räksmörgås\n".getBytes(charset));
            zos.write("Line2\n".getBytes(charset));
            zos.closeEntry();

            zos.finish();
        } catch (Exception e) {
            throw new DataExportException("Cannot create " + exportFile, e);
        }

        if (!tmpFile.renameTo(exportFile)) {
            log.error("Cannot rename {} to {}", tmpFile, exportFile);
            tmpFile.delete();
        }
    }

    private void writeDaemonConfig(OutputStream os, DaemonConfig config, Charset charset) throws IOException, IllegalAccessException {
        Set<String> lines = new TreeSet<>();
        FileUtils.extractFieldValuesFrom(config, lines);
        String NL = "\n";
        for (String line : lines) {
            os.write(line.getBytes(charset));
            os.write(NL.getBytes(charset));
        }
    }

    private File createTempFile(File file) throws DataExportException {
        try {
            return File.createTempFile("codekvast-export", ".tmp", file.getParentFile());
        } catch (IOException e) {
            throw new DataExportException("Cannot create temporary file in " + file.getParentFile(), e);
        }
    }
}
