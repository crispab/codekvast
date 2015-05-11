package se.crisp.codekvast.server.codekvast_server.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Wrapper for environment properties codekvast.*
 *
 * @author olle.hallin@crisp.se
 */
@Component("codekvastSettings")
@ConfigurationProperties(prefix = "codekvast")
@Data
@Slf4j
public class CodekvastSettings {
    private boolean multiTenant = false;
    private int defaultTrulyDeadAfterSeconds = 30 * 24 * 60 * 60;
    private File backupPath = new File("/var/backups/codekvast");
    private int backupSaveGenerations = 5;
    private String applicationName;
    private String displayVersion;
    private String committer;
    private String commitDate;
    private String commitMessage;
    private int eventBusThreads = 0;
    private long statisticsDelayMillis = 5000;

    @PostConstruct
    public void logStartup() {
        System.out.printf("%s v%s (%s) started%n", applicationName, displayVersion, commitDate);
        log.info("{} v{} ({}) starts", applicationName, displayVersion, commitDate);
    }

    public File[] getBackupPaths() {
        File fallbackPath = new File(System.getProperty("java.io.tmpdir"), "codekvast/.backup");
        return new File[]{backupPath, fallbackPath};
    }
}
