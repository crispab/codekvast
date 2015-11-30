package se.crisp.codekvast.warehouse.config;

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
    private String applicationName;
    private String displayVersion;
    private String committer;
    private String commitDate;
    private String commitMessage;

    private File importPath;
    private int importPathPollIntervalSeconds;
    private boolean deleteImportedFiles;

    @PostConstruct
    public void logStartup() {
        System.out.printf("%s v%s (%s) started%n", applicationName, displayVersion, commitDate);
        log.info("{} v{} ({}) starts", applicationName, displayVersion, commitDate);
    }
}
