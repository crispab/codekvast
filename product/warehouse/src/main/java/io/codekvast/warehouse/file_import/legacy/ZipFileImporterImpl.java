/*
 * Copyright (c) 2015-2017 Crisp AB
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
package io.codekvast.warehouse.file_import.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.codekvast.agent.lib.model.v1.legacy.ExportFileMetaInfo;
import io.codekvast.agent.lib.model.v1.legacy.ExportFileEntry;
import io.codekvast.agent.lib.model.v1.legacy.JvmData;
import io.codekvast.agent.lib.model.v1.SignatureStatus;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;
import static java.time.Instant.now;

/**
 * @author olle.hallin@crisp.se
 */
@Slf4j
@Service
public class ZipFileImporterImpl implements ZipFileImporter {

    private final ImportDAO importDAO;
    private final Charset charset = Charset.forName("UTF-8");
    private final ObjectMapper objectMapper;

    @Inject
    public ZipFileImporterImpl(ImportDAO importDAO, ObjectMapper objectMapper) {
        this.importDAO = importDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importZipFile(File file) {
        log.debug("Importing {}", file);
        Instant startedAt = now();

        ExportFileMetaInfo metaInfo = null;
        ImportDAO.ImportContext context = new ImportDAO.ImportContext();

        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
             InputStreamReader reader = new InputStreamReader(zipInputStream, charset)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                ExportFileEntry exportFileEntry = ExportFileEntry.fromString(zipEntry.getName());
                log.debug("Reading {} ...", zipEntry.getName());

                switch (exportFileEntry) {
                case META_INFO:
                    metaInfo = ExportFileMetaInfo.fromInputStream(zipInputStream);
                    if (importDAO.isFileImported(metaInfo)) {
                        log.debug("{} with uuid {} has already been imported", file, metaInfo.getUuid());
                        return;
                    }
                    break;
                case APPLICATIONS:
                    readApplications(reader, context);
                    break;
                case METHODS:
                    readMethods(reader, context);
                    break;
                case JVMS:
                    readJvms(reader, context);
                    break;
                case INVOCATIONS:
                    readInvocations(reader, context);
                    break;
                }
            }
            if (metaInfo != null) {
                importDAO.recordFileAsImported(metaInfo,
                                               ImportDAO.ImportStatistics.builder()
                                                                         .importFile(file)
                                                                         .fileSize(humanReadableByteCount(file.length()))
                                                                         .processingTime(Duration.between(startedAt, now()))
                                                                         .build());
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Cannot import " + file, e);
        }
    }


    private String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exponent = (int) (Math.log(bytes) / Math.log(1000));
        String unit = " kMGTPE".charAt(exponent) + "B";
        return format("%.1f %s", bytes / Math.pow(1000, exponent), unit);
    }

    private void doReadCsv(InputStreamReader reader, String what, Function<String[], Boolean> lineProcessor) {
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        int count = 0;
        Instant startedAt = now();
        for (String[] columns : csvReader) {
            if (lineProcessor.apply(columns)) {
                count += 1;

                if (count % 1000 == 0) {
                    log.debug("Imported {} {}...", count, what);
                }
            }
        }
        log.debug("Imported {} {} in {} ms", count, what, Duration.between(startedAt, now()).toMillis());
    }

    private void readApplications(InputStreamReader reader, ImportDAO.ImportContext context) {
        doReadCsv(reader, "applications", (String[] columns) -> {
            ImportDAO.Application app = ImportDAO.Application.builder()
                                                             .localId(Long.valueOf(columns[0]))
                                                             .name(columns[1])
                                                             .version(columns[2])
                                                             .createdAtMillis(Long.valueOf(columns[3]))
                                                             .build();

            return importDAO.saveApplication(app, context);
        });
    }

    private void readMethods(InputStreamReader reader, ImportDAO.ImportContext context) {
        doReadCsv(reader, "methods", (String[] columns) -> {
            ImportDAO.Method method = ImportDAO.Method.builder()
                                                      .localId(Long.valueOf(columns[0]))
                                                      .visibility(columns[1])
                                                      .signature(columns[2])
                                                      .createdAtMillis(Long.valueOf(columns[3]))
                                                      .declaringType(columns[4])
                                                      .exceptionTypes(columns[5])
                                                      .methodName(columns[6])
                                                      .modifiers(columns[7])
                                                      .packageName(columns[8])
                                                      .parameterTypes(columns[9])
                                                      .returnType(columns[10])
                                                      .build();
            return importDAO.saveMethod(method, context);
        });
    }

    private void readJvms(InputStreamReader reader, ImportDAO.ImportContext context) {
        doReadCsv(reader, "JVMs", (String[] columns) -> doProcessJvm(context, columns));
    }

    @SneakyThrows(IOException.class)
    private boolean doProcessJvm(ImportDAO.ImportContext context, String[] columns) {
        ImportDAO.Jvm jvm = ImportDAO.Jvm.builder()
                                         .localId(Long.valueOf(columns[0]))
                                         .uuid(columns[1])
                                         .startedAtMillis(Long.valueOf(columns[2]))
                                         .dumpedAtMillis(Long.valueOf(columns[3]))
                                         .jvmDataJson(columns[4])
                                         .build();
        JvmData jvmData = objectMapper.readValue(jvm.getJvmDataJson(), JvmData.class);
        return importDAO.saveJvm(jvm, jvmData, context);
    }

    private void readInvocations(InputStreamReader reader, ImportDAO.ImportContext context) {
        doReadCsv(reader, "invocations", (String[] columns) -> {
            ImportDAO.Invocation invocation = ImportDAO.Invocation.builder()
                                                                  .localApplicationId(Long.valueOf(columns[0]))
                                                                  .localMethodId(Long.valueOf(columns[1]))
                                                                  .localJvmId(Long.valueOf(columns[2]))
                                                                  .invokedAtMillis(Long.valueOf(columns[3]))
                                                                  .invocationCount(Long.valueOf(columns[4]))
                                                                  .status(SignatureStatus.fromOrdinal(Integer.valueOf(columns[5])))
                                                                  .build();
            return importDAO.saveInvocation(invocation, context);
        });
    }

}
