package se.crisp.codekvast.warehouse.file_import;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.lib.model.ExportFileMetaInfo;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileEntry;
import se.crisp.codekvast.agent.lib.model.v1.ExportFileFormat;
import se.crisp.codekvast.warehouse.config.CodekvastSettings;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static se.crisp.codekvast.warehouse.file_import.ImportService.*;

/**
 * Scans a certain directory for files produced by the Codekvast daemon, and attempts to import them to the database.
 *
 * @author olle.hallin@crisp.se
 */
@Service
@Slf4j
public class FileImportWorker {

    private final CodekvastSettings codekvastSettings;
    private final ImportService importService;
    private final Charset charset = Charset.forName("UTF-8");

    @Inject
    public FileImportWorker(CodekvastSettings codekvastSettings, ImportService importService) {
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
                    } else if (!file.getName().endsWith(ExportFileFormat.ZIP.getSuffix())) {
                        log.debug("Ignoring {}, can only handle {} files", file, ExportFileFormat.ZIP);
                    } else {
                        importZipFile(file);
                        deleteFile(file);
                    }
                }
            }
        }
    }

    private void deleteFile(File file) {
        boolean deleted = file.delete();
        if (deleted) {
            log.info("Deleted {}", file);
        } else {
            log.warn("Could not delete {}", file);
        }
    }

    @Transactional
    protected void importZipFile(File file) {
        log.debug("Importing {}", file);

        ExportFileMetaInfo metaInfo = null;
        ImportContext context = new ImportContext();

        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
             InputStreamReader reader = new InputStreamReader(zipInputStream, charset)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                ExportFileEntry exportFileEntry = ExportFileEntry.fromString(zipEntry.getName());
                log.debug("Reading {}...", exportFileEntry);

                switch (exportFileEntry) {
                case META_INFO:
                    metaInfo = ExportFileMetaInfo.fromInputStream(zipInputStream);
                    if (importService.isFileImported(metaInfo)) {
                        log.debug("{} with uuid {} has already been imported", file, metaInfo.getUuid());
                        return;
                    }
                    break;
                case APPLICATIONS:
                    readApplicationsCsv(reader, context);
                    break;
                case METHODS:
                    readMethodsCsv(reader, context);
                    break;
                case JVMS:
                    readJvmsCsv(reader, context);
                    break;
                case INVOCATIONS:
                    readInvocationsCsv(reader, context);
                    break;
                }
            }
            if (metaInfo != null) {
                importService.recordFileAsImported(metaInfo.withFileLengthBytes(file.length())
                                                           .withFileName(file.getPath()));
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Cannot import " + file, e);
        }
    }

    private void readApplicationsCsv(InputStreamReader reader, ImportContext context) {
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        for (String[] columns : csvReader) {
            int col = 0;
            Application app = Application.builder()
                                         .localId(Long.valueOf(columns[col++]))
                                         .name(columns[col++])
                                         .version(columns[col++])
                                         .createdAtMillis(Long.valueOf(columns[col++]))
                                         .build();

            importService.saveApplication(app, context);
        }
    }

    private void readMethodsCsv(InputStreamReader reader, ImportContext context) {
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        for (String[] columns : csvReader) {
            int col = 0;
            Method method = Method.builder()
                                  .localId(Long.valueOf(columns[col++]))
                                  .visibility(columns[col++])
                                  .signature(columns[col++])
                                  .createdAtMillis(Long.valueOf(columns[col++]))
                                  .declaringType(columns[col++])
                                  .exceptionTypes(columns[col++])
                                  .methodName(columns[col++])
                                  .modifiers(columns[col++])
                                  .packageName(columns[col++])
                                  .parameterTypes(columns[col++])
                                  .returnType(columns[col++])
                                  .build();
            importService.saveMethod(method, context);
        }
    }

    private void readJvmsCsv(InputStreamReader reader, ImportContext context) {
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        for (String[] columns : csvReader) {
            int col = 0;
            Jvm jvm = Jvm.builder()
                         .localId(Long.valueOf(columns[col++]))
                         .uuid(columns[col++])
                         .startedAtMillis(Long.valueOf(columns[col++]))
                         .dumpedAtMillis(Long.valueOf(columns[col++]))
                         .jvmDataJson(columns[col++])
                         .build();
            importService.saveJvm(jvm, context);
        }
    }

    private void readInvocationsCsv(InputStreamReader reader, ImportContext context) {
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        for (String[] columns : csvReader) {
            int col = 0;
            Invocation invocation = Invocation.builder()
                                              .localApplicationId(Long.valueOf(columns[col++]))
                                              .localMethodId(Long.valueOf(columns[col++]))
                                              .localJvmId(Long.valueOf(columns[col++]))
                                              .invokedAtMillis(getInvokedAtMillis(columns[col++]))
                                              .invocationCount(getInvocationCount(columns[col++]))
                                              .confidence(getConfidence(columns[col++]))
                                              .build();
            importService.saveInvocation(invocation, context);
        }
    }

    private Long getInvocationCount(String value) {
        return value == null || value.isEmpty() || value.equals("0") ? null : Long.valueOf(value);
    }

    private Long getInvokedAtMillis(String value) {
        return value == null || value.isEmpty() || value.equals("-1") ? null : Long.valueOf(value);
    }

    private Byte getConfidence(String value) {
        return value == null || value.isEmpty() ? null : Byte.valueOf(value);
    }

}
