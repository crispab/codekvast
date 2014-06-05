package se.crisp.duck.agent.sensor;

import org.aspectj.lang.Signature;
import se.crisp.duck.agent.util.Configuration;
import se.crisp.duck.agent.util.SensorRun;
import se.crisp.duck.agent.util.SensorUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private final Configuration config;
    private final SensorRun sensorRun;
    private final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());

    private final ConcurrentMap<String, Long> usages = new ConcurrentHashMap<String, Long>();

    private UsageRegistry(Configuration config, SensorRun sensorRun) {
        this.config = config;
        this.sensorRun = sensorRun;

        File sensorsPath = config.getSensorsPath();
        if (sensorsPath != null && !sensorsPath.exists()) {
            sensorsPath.mkdirs();
        }
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(Configuration config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   SensorRun.builder()
                                                            .appName(config.getAppName())
                                                            .hostName(getHostName())
                                                            .uuid(UUID.randomUUID())
                                                            .startedAtMillis(System.currentTimeMillis())
                                                            .build()
        );
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot get local hostname: " + e);
            return "-- unknown --";
        }
    }

    /**
     * This method is invoked by an aspect.
     *
     * It will exclude a certain method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        usages.put(signature.toLongString(), currentTimeMillis.longValue());
    }

    /**
     * This method is invoked by an aspect.
     *
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
    public synchronized void dumpDataToDisk(int dumpCount) {
        dumpSensorRun();
        SensorUtils.dumpUsageData(config.getDataFile(), dumpCount, usages);
        currentTimeMillis.set(System.currentTimeMillis());
    }

    private void dumpSensorRun() {
        File file = config.getSensorFile();
        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            sensorRun.saveTo(tmpFile);
            SensorUtils.renameFile(tmpFile, file);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot save " + file + ": " + e);
        }
    }
}
