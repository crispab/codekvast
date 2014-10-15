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
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentMap<String, Long> usages = new ConcurrentHashMap<String, Long>();

    public UsageRegistry(AgentConfig config, JvmRun jvmRun) {
        this.config = config;
        this.jvmRun = jvmRun;
        this.jvmRunFile = config.getJvmRunFile();
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(AgentConfig config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   JvmRun.builder()
                                                            .hostName(getHostName())
                                                            .uuid(UUID.randomUUID())
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
     * Record that this method signature was invoked at current time.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        usages.put(signature.toLongString(), currentTimeMillis.longValue());
    }

    /**
     * Record that this JPS page was invoked at current time.
     * <p/>
     * Thread-safe.
     */
    public void registerJspPageExecution(String pageName) {
        usages.put(pageName, currentTimeMillis.longValue());
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
            dumpJvmRun();
            FileUtils.writeUsageDataTo(config.getUsageFile(), dumpCount, usages);
            currentTimeMillis.set(System.currentTimeMillis());
        }
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
