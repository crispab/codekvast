package se.crisp.codekvast.daemon.beans;

import lombok.Data;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.shared.model.Jvm;

import java.io.File;
import java.time.Instant;

/**
 * Mutable state for a {@link Jvm} object.
 */
@Data
public class JvmState {
    private Jvm jvm;
    private File invocationsFile;
    private CodeBase codeBase;
    private String appVersion;
    private Instant jvmDataProcessedAt = Instant.MIN;
    private boolean firstRun = true;
    private long databaseAppId;
    private long databaseJvmId;

    public Instant getJvmDumpedAt() {
        return Instant.ofEpochMilli(jvm.getDumpedAtMillis());
    }

    public Instant getJvmStartedAt() {
        return Instant.ofEpochMilli(jvm.getStartedAtMillis());
    }
}
