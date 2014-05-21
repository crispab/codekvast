package duck.spike.sensor;

import duck.spike.util.*;
import org.aspectj.lang.Signature;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olle Hallin
 */
@SuppressWarnings("Singleton")
public class UsageRegistry {
    public static UsageRegistry instance;

    private final Configuration config;
    private final SensorRun sensorRun;

    private final ConcurrentMap<String, Long> usages = new ConcurrentHashMap<String, Long>();

    private UsageRegistry(Configuration config, SensorRun sensorRun) {
        this.config = config;
        this.sensorRun = sensorRun;

        File parent = config.getDataFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Must be called before handing over to the AspectJ load-time weaver.
     */
    public static void initialize(Configuration config) {
        UsageRegistry.instance = new UsageRegistry(config,
                                                   SensorRun.builder()
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
     * This method is invoked by {@link duck.spike.sensor.AbstractDuckAspect#recordMethodCall(org.aspectj.lang.JoinPoint)}.
     * It will exclude a certain method from being reported as useless.
     * <p/>
     * Thread-safe.
     */
    public void registerMethodExecution(Signature signature) {
        String sig = AspectjUtils.makeMethodKey(signature);
        usages.put(sig, System.currentTimeMillis());
    }

    /**
     * Dumps method usage to a file on disk.
     * <p/>
     * Thread-safe.
     */
    public synchronized void dumpDataToDisk(int dumpCount) {
        List<Usage> data = new ArrayList<Usage>();
        for (Map.Entry<String, Long> entry : usages.entrySet()) {
            data.add(new Usage(entry.getKey(), entry.getValue()));
        }

        instance.dumpSensorRun();
        UsageUtils.dumpUsageData(config.getDataFile(), dumpCount, data);
    }

    private void dumpSensorRun() {
        File file = config.getSensorFile();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            sensorRun.saveTo(tmpFile);
            UsageUtils.renameFile(tmpFile, file);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot save " + file + ": " + e);
        }
    }

}
