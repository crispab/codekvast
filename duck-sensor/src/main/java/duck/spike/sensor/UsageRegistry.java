package duck.spike.sensor;

import duck.spike.util.AspectjUtils;
import duck.spike.util.Configuration;
import duck.spike.util.SensorRun;
import duck.spike.util.Usage;
import org.aspectj.lang.Signature;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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

    private final ConcurrentMap<String, Usage> usages = new ConcurrentHashMap<String, Usage>();

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
        usages.put(sig, new Usage(sig, System.currentTimeMillis()));
    }

    /**
     * Dumps method usage to a file on disk.
     * <p/>
     * Thread-safe.
     */
    public synchronized void dumpDataToDisk(int dumpCount) {
        instance.dumpSensorRun(dumpCount);
        instance.dumpUsageData(dumpCount);
    }

    private void dumpSensorRun(int dumpCount) {
        File file = config.getSensorFile();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            sensorRun.saveTo(tmpFile);
            renameFile(tmpFile, file);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot save " + file + ": " + e);
        }
    }

    private void dumpUsageData(int dumpCount) {
        long startedAt = System.currentTimeMillis();
        File file = config.getDataFile();

        try {
            File tmpFile = File.createTempFile("duck", ".tmp", file.getAbsoluteFile().getParentFile());
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));

            Date dumpedAt = new Date();
            out.printf(Locale.ENGLISH, "# Duck usage results #%d for '%s' at %s%n", dumpCount, config.getAppName(), dumpedAt);
            out.println("# lastUsedMillis:signature");

            // Only iterate over trackedMethods once
            Iterable<Usage> usages = new ArrayList<Usage>(this.usages.values());

            int count = 0;
            for (Usage usage : usages) {
                out.println(usage);
                count += 1;
            }

            out.flush();
            out.close();

            renameFile(tmpFile, file);

            long elapsed = System.currentTimeMillis() - startedAt;
            out.printf(Locale.ENGLISH, "# Dump #%d for '%s' at %s took %d ms, number of methods: %d%n", dumpCount, config.getAppName(),
                       dumpedAt, elapsed, count);
        } catch (IOException e) {
            DuckSensor.out.println(DuckSensor.NAME + " cannot dump usage data to " + file + ": " + e);
        }
    }

    private void renameFile(File from, File to) {
        if (!from.renameTo(to)) {
            DuckSensor.out.printf(Locale.ENGLISH, "%s cannot rename %s to %s%n", DuckSensor.NAME, from.getAbsolutePath(),
                              to.getAbsolutePath());
            from.delete();
        }
    }

}
