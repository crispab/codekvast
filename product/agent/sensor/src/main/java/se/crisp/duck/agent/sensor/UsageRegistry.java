package se.crisp.duck.agent.sensor;

import org.aspectj.lang.Signature;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.agent.util.Sensor;
import se.crisp.duck.agent.util.SensorUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
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
        sensorFile.deleteOnExit();
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(AgentConfig config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   Sensor.builder()
                                                         .appName(config.getAppName())
                                                         .hostName(SensorUtils.getHostName())
                                                         .uuid(UUID.randomUUID())
                                                         .startedAtMillis(System.currentTimeMillis())
                                                         .build());
    }

    /**
     * This method is invoked by an aspect.
     * <p/>
     * It will exclude a certain method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        usages.put(signature.toLongString(), currentTimeMillis.longValue());
    }

    /**
     * This method is invoked by an aspect.
     * <p/>
     * It will exclude a certain JSP page from being reported as useless.
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
        File sensorsPath = config.getSensorsPath();
        sensorsPath.mkdirs();
        if (!sensorsPath.exists()) {
            DuckSensor.out.println("Cannot dump usage data, " + sensorsPath + " cannot be created");
        } else {
            dumpSensorRun();
            SensorUtils.dumpUsageData(config.getUsageFile(), dumpCount, usages);
            currentTimeMillis.set(System.currentTimeMillis());
        }
    }

    public AgentConfig getConfig() {
        return config;
    }

    private void dumpSensorRun() {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("duck", ".tmp", sensorFile.getParentFile());
            sensor.saveTo(tmpFile);
            SensorUtils.renameFile(tmpFile, sensorFile);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot save " + sensorFile + ": " + e);
        } finally {
            SensorUtils.safeDelete(tmpFile);
        }
    }

}
