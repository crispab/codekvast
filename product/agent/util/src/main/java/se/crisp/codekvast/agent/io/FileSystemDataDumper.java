package se.crisp.codekvast.agent.io;

import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

public class FileSystemDataDumper implements DataDumper {

    private final CollectorConfig config;
    private final PrintStream out;
    private final File jvmFile;

    public FileSystemDataDumper(CollectorConfig config, PrintStream out) {
        this.config = config;
        this.out = out;
        this.jvmFile = config == null ? null : config.getJvmFile();
    }

    @Override
    public boolean prepareForDump() {
        File outputPath = config.getInvocationsFile().getParentFile();
        outputPath.mkdirs();
        return outputPath.exists();
    }

    @Override
    public void dumpData(Jvm jvm, int dumpCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        dumpJvmData(jvm);
        dumpInvocationData(dumpCount, recordingIntervalStartedAtMillis, invocations);
    }

    private void dumpJvmData(Jvm jvm) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", jvmFile.getParentFile());
            jvm.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, jvmFile);
        } catch (IOException e) {
            out.println("Codekvast cannot save " + jvmFile + ": " + e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }

    }

    private void dumpInvocationData(int dumpCount, long recordingIntervalStartedAtMillis, Set<String> invocations) {
        FileUtils.writeInvocationDataTo(config.getInvocationsFile(), dumpCount, recordingIntervalStartedAtMillis,
                                        invocations);

    }

}
