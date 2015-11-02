package se.crisp.codekvast.daemon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.daemon.beans.JvmState;
import se.crisp.codekvast.daemon.codebase.CodeBase;
import se.crisp.codekvast.daemon.impl.DataExportException;
import se.crisp.codekvast.daemon.impl.DataProcessingException;
import se.crisp.codekvast.daemon.util.LogUtil;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.model.Jvm;
import se.crisp.codekvast.shared.util.FileUtils;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the meat of the codekvast-daemon. It contains a scheduled method that periodically processes data from the collector.
 *
 * @author olle.hallin@crisp.se
 */
@Component
@Slf4j
public class DaemonWorker {

    private final DaemonConfig config;
    private final AppVersionResolver appVersionResolver;
    private final DataProcessor dataProcessor;

    private final Map<String, JvmState> jvmStates = new HashMap<String, JvmState>();
    private final DataExporter dataExporter;

    @Inject
    public DaemonWorker(DaemonConfig config, AppVersionResolver appVersionResolver, DataProcessor dataProcessor,
                        DataExporter dataExporter) {
        this.config = config;
        this.appVersionResolver = appVersionResolver;
        this.dataProcessor = dataProcessor;
        this.dataExporter = dataExporter;

        log.info("{} {} started", getClass().getSimpleName(), config.getDisplayVersion());
    }

    @PreDestroy
    public void shutdownHook() {
        log.info("{} {} shuts down", getClass().getSimpleName(), config.getDisplayVersion());
    }

    @Scheduled(initialDelay = 10L, fixedDelayString = "${codekvast.dataProcessingIntervalSeconds}000")
    public void analyseCollectorData() {
        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(getClass().getSimpleName());
        try {
            doAnalyzeCollectorData();
            doExportData();
        } finally {
            Thread.currentThread().setName(oldThreadName);
        }
    }

    private void doExportData() {
        try {
            dataExporter.exportData();
        } catch (DataExportException e) {
            LogUtil.logException(log, "Could not export data", e);
        }
    }

    private void doAnalyzeCollectorData() {
        log.debug("Analyzing collector data");

        findJvmStates(config.getDataPath());

        for (JvmState jvmState : jvmStates.values()) {
            if (jvmState.isFirstRun()) {
                // The daemon might have crashed between consuming invocation data files and storing them in the database.
                // Make sure that invocation data is not lost...
                FileUtils.resetAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());
                jvmState.setFirstRun(false);
            }
            try {
                dataProcessor.processData(jvmState, new CodeBase(jvmState.getJvm().getCollectorConfig()));
            } catch (DataProcessingException e) {
                LogUtil.logException(log, "Could not process data for " + jvmState.getJvm().getCollectorConfig().getAppName(), e);
            }
        }
    }

    private void findJvmStates(File dataPath) {
        log.debug("Looking for jvm.dat in {}", dataPath);

        File[] files = dataPath.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(CollectorConfig.JVM_BASENAME)) {
                    addOrUpdateJvmState(file);
                } else if (file.isDirectory()) {
                    findJvmStates(file);
                }
            }
        }
    }

    private void addOrUpdateJvmState(File file) {
        try {

            Jvm jvm = Jvm.readFrom(file);

            JvmState jvmState = jvmStates.get(jvm.getJvmUuid());
            if (jvmState == null) {
                jvmState = new JvmState();
                jvmStates.put(jvm.getJvmUuid(), jvmState);
            }
            jvmState.setJvm(jvm);
            appVersionResolver.resolveAppVersion(jvmState);

            jvmState.setInvocationsFile(new File(file.getParentFile(), CollectorConfig.INVOCATIONS_BASENAME));
        } catch (IOException e) {
            log.error("Cannot load " + file, e);
        }
    }
}
