package se.crisp.duck.agent.sensor;

import org.aspectj.lang.Signature;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.agent.util.FileUtils;
import se.crisp.duck.agent.util.Sensor;

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
    private final Sensor sensor;
    private final File sensorFile;
    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentMap<String, Long> usages = new ConcurrentHashMap<String, Long>();

    public UsageRegistry(AgentConfig config, Sensor sensor) {
        this.config = config;
        this.sensor = sensor;
        this.sensorFile = config.getSensorFile();
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(AgentConfig config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   Sensor.builder()
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
            DuckSensor.out.println("Cannot dump usage data, " + outputPath + " cannot be created");
        } else {
            dumpSensorRun();
            FileUtils.writeUsageDataTo(config.getUsageFile(), dumpCount, usages);
            currentTimeMillis.set(System.currentTimeMillis());
        }
    }

    public AgentConfig getConfig() {
        return config;
    }

    /**
     * Dumps data about this sensor run to a disk file.
     */
    private void dumpSensorRun() {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("duck", ".tmp", sensorFile.getParentFile());
            sensor.saveTo(tmpFile);
            FileUtils.renameFile(tmpFile, sensorFile);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot save " + sensorFile + ": " + e);
        } finally {
            FileUtils.safeDelete(tmpFile);
        }
    }

}
