package se.crisp.codekvast.agent.main;

import lombok.Data;
import se.crisp.codekvast.agent.codebase.CodeBase;
import se.crisp.codekvast.agent.model.Jvm;

import java.io.File;

/**
 * Created by olle on 2015-09-14.
 */
@Data
public class JvmState {
    private Jvm jvm;
    private File invocationsFile;
    private CodeBase codeBase;
    private String appVersion;
    private long jvmDataUploadedAt;
    private long codebaseUploadedAt;
    private long invocationDataUploadedAt;
    private boolean firstRun = true;
}
