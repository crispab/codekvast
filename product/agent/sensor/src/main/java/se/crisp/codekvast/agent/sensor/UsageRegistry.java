package se.crisp.codekvast.agent.sensor;

import org.aspectj.lang.Signature;
import se.crisp.codekvast.agent.util.AgentConfig;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.agent.util.JvmRun;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private final AgentConfig config;
    private final JvmRun jvmRun;
    private final File jvmRunFile;

    // We really want to store signature usage in a ConcurrentSet, but that is not available in JDK 1.5 so we use a ConcurrentMap as a
    // set instead, with dummy objects as values. We are only interested in the key set.
    private final Object dummyObject = new Object();

    // Toggle between two usage maps to avoid synchronisation
    private final ConcurrentMap[] usages = new ConcurrentMap[2];
    private volatile int currentUsageIndex = 0;
    private long recordingIntervalStartedAtMillis = System.currentTimeMillis();

    public UsageRegistry(AgentConfig config, JvmRun jvmRun) {
        this.config = config;
        this.jvmRun = jvmRun;
        this.jvmRunFile = config.getJvmRunFile();

        for (int i = 0; i < usages.length; i++) {
            this.usages[i] = new ConcurrentHashMap<String, Object>();
        }
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(AgentConfig config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   JvmRun.builder()
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
        usages[currentUsageIndex].put(signature.toLongString(), dummyObject);
    }

    /**
     * Record that this JPS page was invoked at current recording interval.
     * <p/>
     * Thread-safe.
     */
    public void registerJspPageExecution(String pageName) {
        //noinspection unchecked
        usages[currentUsageIndex].put(pageName, dummyObject);
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
            CodeKvastSensor.out.println("Cannot dump usage data, " + outputPath + " cannot be created");
        } else {
            long oldRecordingIntervalStartedAtMillis = recordingIntervalStartedAtMillis;
            int oldIndex = currentUsageIndex;

            toggleUsageIndex();

            dumpJvmRun();

            //noinspection unchecked
            FileUtils.writeUsageDataTo(config.getUsageFile(), dumpCount, oldRecordingIntervalStartedAtMillis, usages[oldIndex].keySet());

            usages[oldIndex].clear();
        }
    }

    private void toggleUsageIndex() {
        recordingIntervalStartedAtMillis = System.currentTimeMillis();
        currentUsageIndex = currentUsageIndex == 0 ? 1 : 0;
    }

    public AgentConfig getConfig() {
        return config;
    }

    /**
     * Dumps data about this JVM run to a disk file.
     */
    private void dumpJvmRun() {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("codekvast", ".tmp", jvmRunFile.getParentFile());
            jvmRun.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, jvmRunFile);
        } catch (IOException e) {
            CodeKvastSensor.out.println(CodeKvastSensor.NAME + " cannot save " + jvmRunFile + ": " + e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }
    }

}
