/**
 * Copyright (c) 2015-2016 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.crisp.codekvast.agent.daemon.worker.impl;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.util.LogUtil;
import se.crisp.codekvast.agent.daemon.worker.DataExportException;
import se.crisp.codekvast.agent.daemon.worker.DataExporter;
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileEntry;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileFormat;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import javax.inject.Inject;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;
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
@Slf4j
public class ZipFileDataExporterImpl implements DataExporter {

    private static final String SCHEMA_VERSION = "V1";

    private final JdbcTemplate jdbcTemplate;
    private final DaemonConfig config;

    @Inject
    public ZipFileDataExporterImpl(JdbcTemplate jdbcTemplate, DaemonConfig config) {
        this.jdbcTemplate = jdbcTemplate;
        this.config = config;
    }

    @Override
    public Optional<File> exportData() throws DataExportException {
        File exportFile = expandPlaceholders(config.getExportFile());
        if (exportFile == null) {
            log.info("No export file configured, data will not be exported");
            return Optional.empty();
        }

        if (!exportFile.getName().toLowerCase().endsWith(ExportFileFormat.ZIP.getSuffix())) {
            log.error("Can only export to " + ExportFileFormat.ZIP + " format");
            return Optional.empty();
        }

        Instant startedAt = now();

        doExportDataTo(exportFile);

        log.info("Created {} ({}) in {} ms", exportFile, LogUtil.humanReadableByteCount(exportFile.length()),
                 Duration.between(startedAt, now()).toMillis());
        return Optional.of(exportFile);
    }

    private File expandPlaceholders(File file) {
        if (file == null) {
            return null;
        }

        String name = file.getName().replace("#hostname#", getHostname()).replace("#timestamp#", now().toString());

        File parentFile = file.getParentFile();
        return parentFile == null ? new File(name) : new File(parentFile, name);
    }

    private void doExportDataTo(File exportFile) throws DataExportException {
        log.debug("Exporting data to {} ...", exportFile);

        File tmpFile = createTempFile(exportFile);

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)))) {
            String uuid = UUID.randomUUID().toString();
            zip.setComment(format("Export of Codekvast local warehouse for %s at %s, uuid=%s", config.getEnvironment(), Instant.now(),
                                  uuid));

            Charset charset = Charset.forName("UTF-8");
            doExportMetaInfo(zip, charset, ExportFileMetaInfo.builder()
                                                             .uuid(uuid)
                                                             .schemaVersion(SCHEMA_VERSION)
                                                             .daemonVersion(config.getDaemonVersion())
                                                             .daemonVcsId(config.getDaemonVcsId())
                                                             .daemonHostname(getHostname())
                                                             .environment(config.getEnvironment())
                                                             .build());

            CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zip, charset));
            doExportDatabaseTable(zip, csvWriter, "applications", "id", "name", "version", "createdAtMillis");
            doExportDatabaseTable(zip, csvWriter, "methods", "id", "visibility", "signature", "createdAtMillis", "declaringType",
                                  "exceptionTypes", "methodName", "modifiers", "packageName", "parameterTypes", "returnType");
            doExportDatabaseTable(zip, csvWriter, "jvms", "id", "uuid", "startedAtMillis", "dumpedAtMillis", "jvmDataJson");
            doExportDatabaseTable(zip, csvWriter, "invocations", "applicationId", "methodId", "jvmId", "invokedAtMillis", "invocationCount",
                                  "status");

            zip.finish();
        } catch (Exception e) {
            throw new DataExportException("Cannot create " + exportFile, e);
        }

        if (!tmpFile.renameTo(exportFile)) {
            tmpFile.delete();
            throw new DataExportException(format("Cannot rename %s to %s", tmpFile, exportFile));
        }
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "-unknown-";
        }
    }

    private void doExportMetaInfo(ZipOutputStream zip, Charset charset, ExportFileMetaInfo metaInfo)
            throws IOException, IllegalAccessException {
        zip.putNextEntry(ExportFileEntry.META_INFO.toZipEntry());

        Set<String> lines = new TreeSet<>();
        FileUtils.extractFieldValuesFrom(metaInfo, lines);
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
        String sql = asList(columns).stream().collect(joining(", ", "SELECT ", " FROM " + table));
        jdbcTemplate.query(sql, rs -> {
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
            return File.createTempFile("codekvast-data", ".tmp", directory);
        } catch (IOException e) {
            throw new DataExportException("Cannot create temporary file in " + directory, e);
        }
    }
}
