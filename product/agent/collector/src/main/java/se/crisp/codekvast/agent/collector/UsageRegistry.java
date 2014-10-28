package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.Signature;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.model.Jvm;
import se.crisp.codekvast.agent.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This is the target of the method execution recording aspects.
 * <p/>
 * It holds data about method usage, and methods for outputting the usage data to disk.
 *
 * @author Olle Hallin
 */
@SuppressWarnings("Singleton")
public class UsageRegistry {

    public static UsageRegistry instance;

    private final CollectorConfig config;
    private final Jvm jvm;
    private final File jvmRunFile;

    // Toggle between two usage sets to avoid synchronisation
    private final Set[] usages = new Set[2];
    private volatile int currentUsageIndex = 0;
    private long recordingIntervalStartedAtMillis = System.currentTimeMillis();

    public UsageRegistry(CollectorConfig config, Jvm jvm) {
        this.config = config;
        this.jvm = jvm;
        this.jvmRunFile = config.getJvmRunFile();

        for (int i = 0; i < usages.length; i++) {
            this.usages[i] = new ConcurrentSkipListSet<String>();
        }
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(CollectorConfig config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   Jvm.builder()
                                                      .collectorConfig(config)
                                                         .hostName(getHostName())
                                                         .jvmFingerprint(UUID.randomUUID().toString())
                                                         .startedAtMillis(System.currentTimeMillis())
                                                         .build());
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     * Record that this method signature was invoked at current recording interval.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        //noinspection unchecked
        usages[currentUsageIndex].add(signature.toLongString());
    }

    /**
     * Record that this JPS page was invoked at current recording interval.
     * <p/>
     * Thread-safe.
     */
    public void registerJspPageExecution(String pageName) {
        //noinspection unchecked
        usages[currentUsageIndex].add(pageName);
    }

    /**
     * Dumps method usage to a file on disk.
     * <p/>
     * Thread-safe.
     */
    public void dumpDataToDisk(int dumpCount) {
        File outputPath = config.getUsageFile().getParentFile();
        outputPath.mkdirs();
        if (!outputPath.exists()) {
            CodekvastCollector.out.println("Cannot dump usage data, " + outputPath + " cannot be created");
        } else {
            long oldRecordingIntervalStartedAtMillis = recordingIntervalStartedAtMillis;
            int oldIndex = currentUsageIndex;

            toggleUsageIndex();

            dumpJvmRun();

            //noinspection unchecked
            FileUtils.writeUsageDataTo(config.getUsageFile(), dumpCount, oldRecordingIntervalStartedAtMillis,
                                       usages[oldIndex]);

            usages[oldIndex].clear();
        }
    }

    private void toggleUsageIndex() {
        recordingIntervalStartedAtMillis = System.currentTimeMillis();
        currentUsageIndex = currentUsageIndex == 0 ? 1 : 0;
    }

    public CollectorConfig getConfig() {
        return config;
    }

    /**
     * Dumps data about this JVM run to a disk file.
     */
    private void dumpJvmRun() {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", jvmRunFile.getParentFile());
            jvm.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, jvmRunFile);
        } catch (IOException e) {
            CodekvastCollector.out.println(CodekvastCollector.NAME + " cannot save " + jvmRunFile + ": " + e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }
    }

}
